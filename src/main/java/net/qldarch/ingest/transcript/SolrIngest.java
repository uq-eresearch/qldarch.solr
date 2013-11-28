package net.qldarch.ingest.transcript;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;

public class SolrIngest implements IngestStage {
    public static Logger logger = LoggerFactory.getLogger(SolrIngest.class);

    private Configuration configuration;

    public SolrIngest(Configuration configuration) {
        this.configuration = configuration;
    }

    public void ingest() {
        URL solrURL = configuration.getSolrURL();
        File outputDir = configuration.getOutputDir();
        Collection<File> summaryFiles =
            FileUtils.listFiles(outputDir, new String[] {"summary"}, true);

        logger.debug("Ingesting files: {}", summaryFiles);

        for (File summaryFile : summaryFiles) {
            if (!summaryFile.exists()) {
                System.out.println("Summary file " + summaryFile + " went missing");
                continue;
            }

            try {
                performSolrIngest(solrURL, summaryFile, outputDir);
            } catch (IOException ei) {
                System.out.printf("Error while performing Solr Ingest for %s : %s",
                        summaryFile, ei.getMessage());
                logger.warn("Error while performing Solr Ingest for {}", summaryFile, ei);
            }
        }
    }

    private void performSolrIngest(URL solrURL, File summaryFile, File outputDir)
            throws IOException {
        Properties summary = new Properties();
        summary.load(new AutoCloseInputStream(FileUtils.openInputStream(summaryFile)));
        if (!summary.containsKey("solr.input")) {
            System.out.println("No solr.input entry in summary file: " + summaryFile);
            return;
        }

        File inputFile = new File(summary.getProperty("solr.input"));
        if (!inputFile.exists()) {
            System.out.println("Error, " + inputFile + " does not exist");
            return;
        }

        File resultFile =
            new File(outputDir, inputFile.getName().replace("-solr.xml", "-result.xml"));

        if (resultFile.exists()) {
            System.out.println("Error, " + resultFile + " already exists, skipping ingest");
            return;
        }

        summary.setProperty("solr.result",
                resultFile.getAbsoluteFile().getCanonicalFile().toString());

        try {
            submitSolrFile(solrURL, inputFile, resultFile, summary);
        } catch (IOException ei) {
            System.out.println("Failed to process file: " + inputFile + ": " +  ei.getMessage());
            // FIXME: Setup logging and replace this with a dump to the logfile.
            ei.printStackTrace();
            return;
        }

        System.out.printf("Submitted %s to %s result: %s\n", inputFile, solrURL, resultFile);

        try (OutputStream os = FileUtils.openOutputStream(summaryFile)) {
            summary.store(os, new Date().toString());
        }
    }

    private void submitSolrFile(URL solrURL, File inputFile, File resultFile, Properties summary)
            throws IOException {
        HttpURLConnection conn = setupSolrConnection(solrURL, inputFile);
        try {
            OutputStream out = conn.getOutputStream();
            try {
                FileUtils.copyFile(inputFile, out);
                // Note: This kludge is the 'official' workaround from Sun.
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4513568
                InputStream in;
                if (conn.getResponseCode() < 400) {
                    in = conn.getInputStream();
                } else {
                    String errmsg = String.format("Solr returned %d for %s, %s",
                            conn.getResponseCode(), inputFile.toString(),
                            conn.getResponseMessage());

                    logger.warn(errmsg);
                    summary.setProperty("solr.error", errmsg);
                    in = conn.getErrorStream();
                }

                try {
                    logger.debug("Writing result to {}", resultFile);
                    FileUtils.copyInputStreamToFile(in, resultFile);
                } finally {
                    if (in != null) in.close();
                }
            } finally {
                if (out != null) out.close();
            }
        } finally {
            conn.disconnect();
        }
    }

    public HttpURLConnection setupSolrConnection(URL solrURL, File inputFile) throws IOException {
        HttpURLConnection urlc = (HttpURLConnection)solrURL.openConnection();
        urlc.setRequestMethod("POST");
        urlc.setDoOutput(true);
        urlc.setDoInput(true);
        urlc.setUseCaches(false);
        urlc.setAllowUserInteraction(false);
        urlc.setFollowRedirects(false);
        urlc.setRequestProperty("Content-type", "text/xml");
        urlc.setFixedLengthStreamingMode(inputFile.length());

        return urlc;
    }
}

