package net.qldarch.ingest.transcript;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
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
import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;

public class TranscriptExport implements IngestStage {
    public static Logger logger = LoggerFactory.getLogger(TranscriptExport.class);

    private Configuration configuration;
    private Repository myRepository;
    private RepositoryConnection conn;
    private Exception initError;

    public static String TRANSCRIPT_QUERY =
        " prefix qldarch: <http://qldarch.net/ns/rdf/2012-06/terms#>" +
        " select ?interview ?transcript ?tloc ?srcfile where {" + 
        "   graph <http://qldarch.net/ns/omeka-export/2013-02-06> {" +
        "     ?interview a qldarch:Interview ." + 
        "     ?transcript a qldarch:Transcript ." +
        "     ?interview qldarch:hasTranscript ?transcript ." +
        "     ?transcript qldarch:systemLocation ?tloc ." +
        "     ?transcript qldarch:sourceFilename ?srcfile ." +
        "   }" +
        " }";

    public TranscriptExport(Configuration configuration) {
        this.configuration = configuration;
    }

    public void ingest() {
        try {
            logger.warn("Connecting to: " + configuration.getEndpoint());
            logger.warn("Repository: " + configuration.getRepository());

            File output = new File(configuration.getOutputDir(), "transcripts");
            output.mkdirs();

            myRepository = new HTTPRepository(configuration.getEndpoint(),
                    configuration.getRepository());
            myRepository.initialize();

            conn = myRepository.getConnection();

            TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, TRANSCRIPT_QUERY).evaluate();
            while (result.hasNext()) {
                BindingSet bs = result.next();
                Value interview = bs.getValue("interview");
                Value transcript = bs.getValue("transcript");
                Value location = bs.getValue("tloc");
                Value sourceFilename = bs.getValue("srcfile");
                if (!(location instanceof Literal)) {
                    System.out.println("location(" + location.toString() + ") not literal");
                } else if (!(sourceFilename instanceof Literal)) {
                    System.out.println("sourceFilename(" + sourceFilename.toString() + ") not literal");
                } else if (!(interview instanceof URI)) {
                    System.out.println("interview(" + interview.toString() + ") not URI");
                } else if (!(transcript instanceof URI)) {
                    System.out.println("transcript(" + transcript.toString() + ") not URI");
                } else {
                    try {
                        URL interviewURL = new URL(interview.toString());
                        URL transcriptURL = new URL(transcript.toString());
                        String locationString = ((Literal)location).getLabel();
                        String srcString = ((Literal)sourceFilename).getLabel();

                        writeSummaryFile(interviewURL, transcriptURL, locationString, srcString);

                        URL locationURL = new URL(new URL(configuration.getArchivePrefix()),
                                ((Literal)location).getLabel());

                        TranscriptParser parser = new TranscriptParser(locationURL.openStream());
                        try {
                            parser.parse();
                        } catch (IllegalStateException ei) {
                            System.out.println(interview.toString() + " " + transcript.toString() + " "
                                   + locationURL.toString() + " " + ei.getMessage());
                            continue;
                        }

                        System.out.println(interview.toString() + " " +
                                transcript.toString() + " " +
                                parser.getTitle());
                        writeJsonTranscript(srcString, parser);
                        writeSolrIngest(srcString, interviewURL, transcriptURL, parser);
                    } catch (MalformedURLException em) {
                        System.out.println("archivePrefix( " + configuration.getArchivePrefix() + " and location(" + location.toString() + ") not valid URL: " + em.getMessage());
                        em.printStackTrace();
                        continue;
                    } catch (IOException ei) {
                        System.out.println("IO error processing interview(" + interview.toString() + "): " + ei.getMessage());
                        ei.printStackTrace();
                    }
                }
            }
        } catch (MalformedQueryException em) {
            em.printStackTrace();
        } catch (RepositoryException er) {
            er.printStackTrace();
        } catch (QueryEvaluationException eq) {
            eq.printStackTrace();
        }
    }

    private void writeSummaryFile(URL interview, URL transcript, String location, String source)
            throws IOException {
        File summaryFile = new File(configuration.getOutputDir(),
                FilenameUtils.getBaseName(source) + ".summary");
        PrintWriter pw = new PrintWriter(FileUtils.openOutputStream(summaryFile));
        pw.printf("%s:%s\n", "interview", interview.toString());
        pw.printf("%s:%s\n", "transcript", transcript.toString());
        pw.printf("%s:%s\n", "location", location.toString());
        pw.printf("%s:%s\n", "source", source.toString());
        pw.flush();
        pw.close();
    }

    private void writeJsonTranscript(String source, TranscriptParser parser) throws IOException {
        File jsonFile = new File(configuration.getOutputDir(),
                FilenameUtils.getBaseName(source) + ".json");
        if (jsonFile.exists()) {
            System.out.println("Error, " + jsonFile + " already exists");
            return;
        }
        PrintStream ps = new PrintStream(FileUtils.openOutputStream(jsonFile));
        parser.printJson(ps);
        ps.flush();
        ps.close();
    }

    private void writeSolrIngest(String source, URL interview, URL transcript,
            TranscriptParser parser) throws IOException {
        File xmlFile = new File(configuration.getOutputDir(),
                FilenameUtils.getBaseName(source) + "-solr.xml");
        if (xmlFile.exists()) {
            System.out.println("Error, " + xmlFile + " already exists");
            return;
        }

        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("add")
            .addAttribute("commitWithin", "30000")
            .addAttribute("overwrite", configuration.getSolrOverwrite() ? "true" : "false");

        for (TranscriptParser.Utterance entry : parser.getInterview()) {
            Element doc = root.addElement("doc");
            doc.addElement("field")
                .addAttribute("name", "id")
                .addText(transcript.toString() + "#" + entry.getTimestamp());
            doc.addElement("field")
                .addAttribute("name", "interview")
                .addText(interview.toString());
            doc.addElement("field")
                .addAttribute("name", "transcript")
                .addText(entry.getUtterance());
        }

        XMLWriter writer = new XMLWriter(FileUtils.openOutputStream(xmlFile),
                OutputFormat.createPrettyPrint());
        writer.write(document);
        writer.close();
    }
}
