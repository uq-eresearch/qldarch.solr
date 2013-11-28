package net.qldarch.ingest.transcript;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TranscriptDeploy implements IngestStage {
    public static Logger logger = LoggerFactory.getLogger(TranscriptDeploy.class);

    private Configuration configuration;

    public TranscriptDeploy(Configuration configuration) {
        this.configuration = configuration;
    }

    public void ingest() {
        File webBase = configuration.getWebBase();
        File jsonPath = configuration.getJsonPath();
        File outputDir = configuration.getOutputDir();
        boolean forceDeploy = configuration.getForceDeploy();

        Collection<File> summaryFiles =
            FileUtils.listFiles(outputDir, new String[] {"summary"}, true);

        logger.debug("Deploying files: {}", summaryFiles);

        for (File summaryFile : summaryFiles) {
            if (!summaryFile.exists()) {
                System.out.println("Summary file " + summaryFile + " went missing");
                continue;
            }

            try {
                performTranscriptDeploy(webBase, jsonPath, summaryFile, outputDir, forceDeploy);
            } catch (IOException ei) {
                System.out.printf("Error while performing Transcript Deploy for %s : %s",
                        summaryFile, ei.getMessage());
                logger.warn("Error while performing Transcript Deploy for {}", summaryFile, ei);
            }
        }
    }

    private void performTranscriptDeploy(File webBase, File jsonPath, File summaryFile,
            File outputDir, boolean force) throws IOException {
        Properties summary = new Properties();
        summary.load(new AutoCloseInputStream(FileUtils.openInputStream(summaryFile)));
        if (!summary.containsKey("json")) {
            System.out.println("No json entry in summary file: " + summaryFile);
            return;
        }

        File inputFile = new File(summary.getProperty("json"));
        if (!inputFile.exists()) {
            System.out.println("Error, " + inputFile + " does not exist");
            return;
        }

        File destFile = webBase.toPath()
            .resolve(jsonPath.toPath())
            .resolve(inputFile.getName().replace(" ", "_")).toFile();

        if (destFile.exists()) {
            if (force) {
                System.out.printf("Destination %s already exists, overwriting\n", destFile);
            } else {
                System.out.printf("Error, %s already exists, skipping\n", destFile);
                return;
            }
        }

        try {
            Files.copy(inputFile.toPath(), destFile.toPath(), REPLACE_EXISTING);
            summary.setProperty("deploy.file",
                    destFile.getAbsoluteFile().getCanonicalFile().toString());
        } catch (IOException ei) {
            String msg = String.format("Failed to deploy file: %s", inputFile);
            System.out.println(msg + ": " +  ei.getMessage());
            logger.error(msg, ei);

            return;
        }

        System.out.printf("Deployed %s to %s\n", inputFile, destFile);

        try (OutputStream os = FileUtils.openOutputStream(summaryFile)) {
            summary.store(os, new Date().toString());
        }
    }
}

