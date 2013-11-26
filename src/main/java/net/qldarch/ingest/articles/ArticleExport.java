package net.qldarch.ingest.articles;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.qldarch.av.parser.TranscriptParser;
import net.qldarch.ingest.archive.ArchiveFile;
import net.qldarch.ingest.archive.ArchiveFileNotFoundException;
import net.qldarch.ingest.archive.ArchiveFiles;
import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;

public class ArticleExport implements IngestStage {
    public static Logger logger = LoggerFactory.getLogger(ArticleExport.class);

    private Configuration configuration;
    private Repository myRepository;
    private RepositoryConnection conn;
    private Exception initError;
    private File outputDir;

    public static String OBJECT_WITH_FILE_QUERY =
        " prefix qldarch: <http://qldarch.net/ns/rdf/2012-06/terms#>" +
        " prefix dcterms:<http://purl.org/dc/terms/>" +
        " select distinct ?item ?title ?periodical where {" + 
        "   graph <http://qldarch.net/ns/omeka-export/2013-02-06> {" +
        "     ?item a qldarch:Article ." + 
        "     ?item qldarch:hasFile _:dontcare ." +
        "     OPTIONAL { ?item dcterms:title ?title } ." +
        "     OPTIONAL { ?item qldarch:periodicalTitle ?periodical } ." +
        "   }" +
        " }";

    public static String FILE_FOR_OBJECT_QUERY =
        " prefix qldarch: <http://qldarch.net/ns/rdf/2012-06/terms#>" +
        " select ?file ?sysloc ?srcfile ?mimetype where {" + 
        "   graph <http://qldarch.net/ns/omeka-export/2013-02-06> {" +
        "     <%~item~%> qldarch:hasFile ?file ." +
        "     ?file qldarch:systemLocation ?sysloc ." +
        "     ?file qldarch:sourceFilename ?srcfile ." +
        "     ?file qldarch:basicMimeType ?mimetype ." +
        "   }" +
        " }";

    public static class SummaryFileExistsException extends Exception {
        public SummaryFileExistsException(String message) {
            super(message);
        }
    }

    public ArticleExport(Configuration configuration) {
        this.configuration = configuration;
    }

    public static Function<Value,String> Value_StringValue = new Function<Value,String>() {
        public String apply(Value value) {
            return value.stringValue();
        }
    };

    public void ingest() {
        try {
            logger.info("Connecting to: " + configuration.getEndpoint());
            logger.info("Repository: " + configuration.getRepository());

            outputDir = new File(configuration.getOutputDir(), "articles");
            outputDir.mkdirs();

            myRepository = new HTTPRepository(configuration.getEndpoint(),
                    configuration.getRepository());
            myRepository.initialize();

            conn = myRepository.getConnection();

            logger.debug("Performing query: {} ", OBJECT_WITH_FILE_QUERY);
            TupleQueryResult interviewResult = conn.prepareTupleQuery(QueryLanguage.SPARQL, OBJECT_WITH_FILE_QUERY).evaluate();
            while (interviewResult.hasNext()) {
                BindingSet ibs = interviewResult.next();
                Value item = ibs.getValue("item");
                Value titleValue = ibs.getValue("title");
                Value periodicalValue = ibs.getValue("periodical");

                logger.trace("Retrieved item result: {}, {}, {}", item, titleValue, periodicalValue);

                ArchiveFiles archiveFiles = new ArchiveFiles();

                Optional<String> title = Optional.fromNullable(titleValue).transform(Value_StringValue);
                Optional<String> periodical = Optional.fromNullable(periodicalValue).transform(Value_StringValue);

                if (!(item instanceof URI)) {
                    logger.warn("item({}) not URI", item);
                } else {
                    try {
                        String queryString = FILE_FOR_OBJECT_QUERY.replace("%~item~%", item.toString());
                        TupleQueryResult fileResult = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString).evaluate();
                        if (!fileResult.hasNext()) {
                            System.out.println("Error: no results found for query: " + queryString);
                            continue;
                        }
                        while (fileResult.hasNext()) {
                            BindingSet fbs = fileResult.next();
                            Value file = fbs.getValue("file");
                            Value location = fbs.getValue("sysloc");
                            Value sourceFilename = fbs.getValue("srcfile");
                            Value mimetype = fbs.getValue("mimetype");

                            if (!(file instanceof URI)) {
                                System.out.println("file(" + file.toString() + ") not URI");
                            } else if (!(location instanceof Literal)) {
                                System.out.println("location(" + location.toString() + ") not literal");
                            } else if (!(sourceFilename instanceof Literal)) {
                                System.out.println("sourceFilename(" + sourceFilename.toString() + ") not literal");
                            } else if (!(mimetype instanceof Literal)) {
                                System.out.println("mimetype(" + mimetype.toString() + ") not literal");
                            }

                            archiveFiles.add(new ArchiveFile(
                                item.toString(),
                                new java.net.URI(file.toString()),
                                ((Literal)location).getLabel(),
                                ((Literal)sourceFilename).getLabel(),
                                ((Literal)mimetype).getLabel()));
                        }
                        fileResult.close();

                        try {
                            URL itemURL = new URL(item.toString());

                            writeSummaryFile(itemURL, title, periodical, archiveFiles);

                            Optional<ArchiveFile> file =
                                archiveFiles.firstByMimeType("text/plain").or(
                                archiveFiles.firstByMimeType("text/rtf")).or(
                                archiveFiles.firstByMimeType("application/pdf"));

                            ArchiveFile sourceFile = null;
                            String bodytext = null;
                            if (file.isPresent()) {
                                sourceFile = file.get();
                                bodytext = sourceFile.toText(configuration.getArchivePrefix());
                            } else {
                                logger.info("Unable to find suitable file for {}, files found: {}\n", itemURL, archiveFiles);
                                continue;
                            }

                            writeSolrIngest(sourceFile, itemURL, bodytext, title, periodical);
                        } catch (MalformedURLException em) {
                            em.printStackTrace();
                        } catch (ArchiveFileNotFoundException ea) {
                            ea.printStackTrace();
                        }
                    } catch (URISyntaxException eu) {
                        eu.printStackTrace();
                    } catch (MalformedQueryException em) {
                        em.printStackTrace();
                    } catch (RepositoryException er) {
                        er.printStackTrace();
                    } catch (QueryEvaluationException eq) {
                        eq.printStackTrace();
                    } catch (IOException ei) {
                        System.out.println("IO error processing article(" + item.toString() + "): " + ei.getMessage());
                        ei.printStackTrace();
                    } catch (SummaryFileExistsException es) {
                        logger.warn("Summary File {} already exists", es.getMessage());
                        continue;
                    }
                }
            }
            interviewResult.close();
        } catch (MalformedQueryException em) {
            em.printStackTrace();
        } catch (RepositoryException er) {
            er.printStackTrace();
        } catch (QueryEvaluationException eq) {
            eq.printStackTrace();
        }
    }

    private String urlToFilename(URL url) {
        try {
            return URLEncoder.encode(url.toString(), Charsets.US_ASCII.name());
        } catch (UnsupportedEncodingException eu) {
            throw new IllegalStateException("US_ASCII returned unsupported", eu);
        }
    }

    private void writeSummaryFile(URL item, Optional<String> title, Optional<String> periodical, ArchiveFiles afs)
            throws IOException, ArchiveFileNotFoundException, SummaryFileExistsException {
        File summaryFile = new File(outputDir, urlToFilename(item) + ".summary");
        if (summaryFile.exists()) {
            throw new SummaryFileExistsException(summaryFile.toString());
        }
        PrintWriter pw = new PrintWriter(FileUtils.openOutputStream(summaryFile));
        pw.printf("%s:%s\n", "article", item.toString());
        pw.printf("%s:%s\n", "title", title.or(""));
        pw.printf("%s:%s\n", "periodical", periodical.or(""));
        for (ArchiveFile af : afs) {
            pw.printf("file: %s, %s, %s, %s\n", af.fileURI.toString(), af.location, af.sourceFile, af.mimetype);
        }
        pw.flush();
        pw.close();
    }

    private void writeSolrIngest(ArchiveFile source, URL article, String bodytext,
            Optional<String> title, Optional<String> periodical) throws IOException {
        File xmlFile = new File(outputDir, urlToFilename(article) + "-solr.xml");
        if (xmlFile.exists()) {
            logger.info("Error, {} already exists", xmlFile);
            return;
        }

        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("add")
            .addAttribute("commitWithin", "30000")
            .addAttribute("overwrite", configuration.getSolrOverwrite() ? "true" : "false");

        Element doc = root.addElement("doc");
        doc.addElement("field")
            .addAttribute("name", "id")
            .addText(article.toString());
        doc.addElement("field")
            .addAttribute("name", "article")
            .addText(bodytext.toString());
        if (title.isPresent()) {
            doc.addElement("field")
                .addAttribute("name", "title")
                .addText(title.get());
        }
        if (title.isPresent()) {
            doc.addElement("field")
                .addAttribute("name", "periodical")
                .addText(periodical.get());
        }
        doc.addElement("field")
            .addAttribute("name", "system_location")
            .addText(source.location);
        doc.addElement("field")
            .addAttribute("name", "original_filename")
            .addText(source.sourceFile);

        XMLWriter writer = new XMLWriter(FileUtils.openOutputStream(xmlFile),
                OutputFormat.createPrettyPrint());
        writer.write(document);
        writer.close();
    }
}
