package net.qldarch.ingest;

import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {


    public static void main(String[] args) {
        Options options = new Options(); 
        try {
            CommandLineParser parser = new GnuParser();

            options.addOption(OptionBuilder
                    .withLongOpt("type")
                    .withDescription("Type of digital object to extract. Valid values are " +
                        "rdf:type URI's or shortnames which assume the qldarch ontology prefix")
                    .hasArgs()
                    .withValueSeparator(',')
                    .create("t"));
            options.addOption(OptionBuilder
                    .withLongOpt("help")
                    .withDescription("Display this message")
                    .create("h"));

            CommandLine cmdline = parser.parse(options, args);

            if (cmdline.hasOption("help")) {
                printHelp(options);
            } else {
                System.out.println("Received arguments: " +
                    Arrays.toString(cmdline.getOptionValues("type")));
            }
        } catch (ParseException em) {
            System.err.println(em.getMessage());

            PrintWriter pw = new PrintWriter(System.err);
            new HelpFormatter().printUsage(pw, 80, "qldarch-ingest", options);
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.flush();
            System.out.flush();
        }
    }

    public static void printHelp(Options options) {
        PrintWriter pw = new PrintWriter(System.err);
        new HelpFormatter().printHelp(pw, 80,
                "java -jar qldarch-ingest.jar [OPTIONS]\n",
                "Digital Archive of Queensland Architecture Ingest Tool\n",
                options, 5, 3, "");
        pw.flush();
    }
}
