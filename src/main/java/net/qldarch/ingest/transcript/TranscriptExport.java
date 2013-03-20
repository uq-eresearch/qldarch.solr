package net.qldarch.ingest.transcript;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Literal;
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
        " select ?interview ?transcript ?tloc where {" + 
        "   graph <http://qldarch.net/ns/omeka-export/2013-02-06> {" +
        "     ?interview a qldarch:Interview ." + 
        "     ?transcript a qldarch:Transcript ." +
        "     ?interview qldarch:hasTranscript ?transcript ." +
        "     ?transcript qldarch:systemLocation ?tloc ." +
        "   }" +
        " }";

    public TranscriptExport(Configuration configuration) {
        this.configuration = configuration;
    }

    public void ingest() {
        try {
            logger.warn("Connecting to: " + configuration.getEndpoint());
            logger.warn("Repository: " + configuration.getRepository());

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
                if (location instanceof Literal) {
                    URL locationURL = new URL(new URL(configuration.getArchivePrefix()),
                            ((Literal)location).getLabel());
                    TranscriptParser parser = new TranscriptParser(locationURL.openStream());
                    try {
//                        throw new IllegalStateException("foo");
                        parser.parse();
                    } catch (IllegalStateException ei) {
                        System.out.println(interview.toString() + " " + transcript.toString() + " "
                               + locationURL.toString() + " " + ei.getMessage());
                        continue;
                    }

                    System.out.println(interview.toString() + " " + transcript.toString() + " " + parser.getTitle());
                } else {
                    System.out.println("location not literal");
                }
            }
        } catch (RepositoryException er) {
            er.printStackTrace();
        } catch (QueryEvaluationException eq) {
            eq.printStackTrace();
        } catch (MalformedQueryException em) {
            em.printStackTrace();
        } catch (MalformedURLException em) {
            em.printStackTrace();
        } catch (IOException ei) {
            ei.printStackTrace();
        }
    }
}
