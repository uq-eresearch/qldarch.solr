package net.qldarch.ingest.transcript;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;

public class SolrIngest implements IngestStage {
    private Configuration configuration;

    public SolrIngest(Configuration configuration) {
        this.configuration = configuration;
    }

    public void ingest() {
        URL solrURL = configuration.getSolrURL();
        File outputDir = configuration.getOutputDir();
        Collection<File> inputFiles = FileUtils.listFiles(outputDir,
                new SuffixFileFilter("-solr.xml"),
                FalseFileFilter.FALSE);

        for (File inputFile : inputFiles) {
            if (!inputFile.exists()) {
                System.out.println("Error, " + inputFile + " does not exist");
                continue;
            }

            File resultFile = generateResultFile(inputFile);
            if (resultFile.exists()) {
                System.out.println("Error, " + resultFile + " already exists");
                continue;
            }

            try {
                HttpURLConnection conn = setupSolrConnection(solrURL, inputFile);
                try {
                    OutputStream out = conn.getOutputStream();
                    try {
                        FileUtils.copyFile(inputFile, out);
                        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            System.out.println("Solr returned " + conn.getResponseCode() + " for " +
                                    inputFile + ", " + conn.getResponseMessage());
                        }

                        InputStream in = conn.getInputStream();
                        try {
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
            } catch (IOException ei) {
                System.out.println("Failed to process file: " + inputFile + ": " +  ei.getMessage());
                // FIXME: Setup logging and replace this with a dump to the logfile.
                ei.printStackTrace();
                continue;
            }

            System.out.println("Submitted " + inputFile + " to " + solrURL + " result: " + resultFile);
        }
    }

    public File generateResultFile(File inputFile) {
        return new File(inputFile.getName().replace("-solr.xml", "-result.xml"));
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

