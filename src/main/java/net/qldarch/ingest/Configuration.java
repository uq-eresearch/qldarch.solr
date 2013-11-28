package net.qldarch.ingest;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;


public class Configuration {
    public static String DEFAULT_ENDPOINT = "http://localhost:8080/openrdf-sesame";
    public static String DEFAULT_REPOSITORY = "QldarchMetadataServer";
    public static String DEFAULT_ARCHIVE_PREFIX = "http://qldarch.net/omeka/archive/files/";
    public static String DEFAULT_SOLR_URL = "http://localhost:8080/solr/update";
    public static String DEFAULT_WEB_BASE = "/var/www/html";
    public static String DEFAULT_JSON_PATH = "static/transcripts/json";

    private File outputdir;
    private List<String> stages;
    private String endpoint;
    private String repository;
    private String archivePrefix;
    private URL solrURL;
    private boolean solrOverwrite;
    private File webBase;
    private File jsonPath;
    private boolean forceDeploy;

    public Configuration(CommandLine commandLine, Set<String> validStages)
            throws ConfigurationException {
        try {
            this.outputdir = new File(fetchMandatory(commandLine, "outputdir"));
            this.stages = fetchMandatoryList(commandLine, "stages");
            this.endpoint = fetchWithDefault(commandLine, "endpoint", DEFAULT_ENDPOINT);
            this.repository = fetchWithDefault(commandLine, "repository", DEFAULT_REPOSITORY);
            this.archivePrefix = fetchWithDefault(commandLine, "archive", DEFAULT_ARCHIVE_PREFIX);
            this.solrURL = new URL(fetchWithDefault(commandLine, "solrurl", DEFAULT_SOLR_URL));
            this.solrOverwrite = commandLine.hasOption("solroverwrite");
            this.webBase = new File(fetchWithDefault(commandLine, "webbase", DEFAULT_WEB_BASE));
            this.jsonPath = new File(fetchWithDefault(commandLine, "jsonpath", DEFAULT_JSON_PATH));
            this.forceDeploy = commandLine.hasOption("forcedeploy");

            validateStages(stages, validStages);
        } catch (Exception e) {
            if (e instanceof ConfigurationException) throw (ConfigurationException)e;
            // FIXME: This needs to be extended with commons-validator
            // http://commons.apache.org/proper/commons-validator/
            throw new ConfigurationException("Invalid argument", e);
        }
    }

    public File getOutputDir() { return outputdir; }
    public List<String> getStages() { return stages; }
    public String getEndpoint() { return endpoint; }
    public String getRepository() { return repository; }
    public String getArchivePrefix() { return archivePrefix; }
    public URL getSolrURL() { return solrURL; }
    public boolean getSolrOverwrite() { return solrOverwrite; }
    public File getWebBase() { return webBase; }
    public File getJsonPath() { return jsonPath; }
    public boolean getForceDeploy() { return forceDeploy; }

    public static String fetchWithDefault(CommandLine commandLine, String option, String defult) {
        if (commandLine.hasOption(option)) {
            return commandLine.getOptionValue(option, defult);
        } else {
            return defult;
        }
    }

    public static List<String> fetchMandatoryList(CommandLine commandLine, String option)
            throws ConfigurationException {
        if (!commandLine.hasOption(option)) {
            throw new ConfigurationException("Option '" + option + "' required.");
        }
        String[] values = commandLine.getOptionValues(option);
        if (values.length == 0) {
            throw new ConfigurationException("Option '" + option + "' requires at least one argument.");
        }
        return Arrays.asList(values);
    }

    public static String fetchMandatory(CommandLine commandLine, String option)
            throws ConfigurationException {
        if (!commandLine.hasOption(option)) {
            throw new ConfigurationException("Option '" + option + "' required.");
        }
        String value = commandLine.getOptionValue(option);
        if (value == null) {
            throw new ConfigurationException("Option '" + option + "' requires an argument.");
        }
        return value;
    }

    public static void validateStages(List<String> stages, Set<String> validStages) 
            throws ConfigurationException {
        for (String stage : stages) {
            if (!validStages.contains(stage)) {
                throw new ConfigurationException("Unknown stage specified: " + stage);
            }
        }
    }
}
