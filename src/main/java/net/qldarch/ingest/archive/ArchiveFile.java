package net.qldarch.ingest.archive;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParsingReader;
import org.apache.tika.parser.ErrorParser;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.rtf.RTFParser;
import org.apache.tika.parser.txt.TXTParser;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveFile {
    public static Logger logger = LoggerFactory.getLogger(ArchiveFile.class);

    public final String item;
    public final URI fileURI;
    public final String location;
    public final String sourceFile;
    public final String mimetype;
    public final String contact;

    public ArchiveFile(String item, URI fileURI, String location,
            String sourceFile, String mimetype, String contact) {
        this.item = item;
        this.fileURI = fileURI;
        this.location = location;
        this.sourceFile = sourceFile;
        this.mimetype = mimetype;
        this.contact = contact;
    }

    public String getContact() {
    	return contact;
    }
    
    public String toString() {
        return String.format("ArchiveFile(%s, %s, %s, %s, %s)",
                item, fileURI, location, sourceFile, mimetype);
    }

    public Reader toReader(String archivePrefix) throws IOException, MalformedURLException {
        logger.debug("Processing {} using prefix {}", this, archivePrefix);

        Parser fileParser = newTextExtractionParser();

        URL fileURL = new URL(new URL(archivePrefix), location);
        InputStream is = fileURL.openStream();
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, mimetype);

        return new ParsingReader(fileParser, is, metadata, new ParseContext());
    }

    public String toText(String archivePrefix) throws IOException, MalformedURLException {
        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(toReader(archivePrefix));
            String result = CharStreams.toString(reader);
            logger.debug("Processing of {} returned {}...", item, truncate(result, 20));

            return result;
        } catch (MalformedURLException e) {
            throw closer.rethrow(e, MalformedURLException.class);
        } finally {
            closer.close();
        }
    }

    private Parser newTextExtractionParser() {
        switch (mimetype) {
            case "text/plain":
               return new TXTParser();
            case "text/rtf":
               return new RTFParser();
            case "application/pdf":
               return new PDFParser();
            case "application/msword":
               return new OfficeParser();
            case "application/zip":
               if (sourceFile.endsWith(".docx")) {
                   return new OOXMLParser();
               } else {
                   return ErrorParser.INSTANCE;
               }
            default:
               return ErrorParser.INSTANCE;
        }
    }

    private static String truncate(String str, int len) {
        return str.length() > len ? str.substring(0, len) : str;
    }
}
