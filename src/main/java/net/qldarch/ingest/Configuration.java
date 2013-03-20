package net.qldarch.ingest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;


public class Configuration {
    public static String DEFAULT_ENDPOINT = "http://localhost:8080/openrdf-sesame";
    public static String DEFAULT_REPOSITORY = "QldarchMetadataServer";
    public static String DEFAULT_ARCHIVE_PREFIX = "http://qldarch.net/omeka/archive/files/";

    private File outputdir;
    private List<String> stages;
    private String endpoint;
    private String repository;
    private String archivePrefix;

    public Configuration(CommandLine commandLine) throws ParseException {
        this.outputdir = new File(fetchMandatory(commandLine, "outputdir"));
        this.stages = fetchMandatoryList(commandLine, "stages");
        this.endpoint = fetchWithDefault(commandLine, "endpoint", DEFAULT_ENDPOINT);
        this.repository = fetchWithDefault(commandLine, "repository", DEFAULT_REPOSITORY);
        this.archivePrefix = fetchWithDefault(commandLine, "archive", DEFAULT_ARCHIVE_PREFIX);
    }

    public File getOutputDir() { return outputdir; }
    public List<String> getStages() { return stages; }
    public String getEndpoint() { return endpoint; }
    public String getRepository() { return repository; }
    public String getArchivePrefix() { return archivePrefix; }

    public static String fetchWithDefault(CommandLine commandLine, String option, String defult) {
        if (commandLine.hasOption(option)) {
            return commandLine.getOptionValue(option, defult);
        } else {
            return defult;
        }
    }

    public static List<String> fetchMandatoryList(CommandLine commandLine, String option)
            throws ParseException {
        if (!commandLine.hasOption(option)) {
            throw new ParseException("Option '" + option + "' required.");
        }
        String[] values = commandLine.getOptionValues(option);
        if (values.length == 0) {
            throw new ParseException("Option '" + option + "' requires at least one argument.");
        }
        return Arrays.asList(values);
    }

    public static String fetchMandatory(CommandLine commandLine, String option)
            throws ParseException {
        if (!commandLine.hasOption(option)) {
            throw new ParseException("Option '" + option + "' required.");
        }
        String value = commandLine.getOptionValue(option);
        if (value == null) {
            throw new ParseException("Option '" + option + "' requires an argument.");
        }
        return value;
    }
}
