package net.qldarch.service.rdf;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static net.qldarch.service.rdf.KnownURIs.*;

public class SesameDataStoreDao implements RdfDataStoreDao {
    public static Logger logger = LoggerFactory.getLogger(SesameDataStoreDao.class);

    private SesameConnectionPool connectionPool;

    public SesameDataStoreDao() {
        this.connectionPool = null;
    }

    public SesameDataStoreDao(String endpoint, String repository) {
        this.connectionPool = getConnectionPool();
        this.connectionPool.setServerURI(endpoint);
        this.connectionPool.setRepoName(repository);
    }

    public SesameDataStoreDao(SesameConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void performInsert(final RdfDescription rdf, final User user,
            final URI hasGraphPredicate, final URI graph) throws MetadataRepositoryException {
        this.getConnectionPool().performOperation(new RepositoryOperation() {
            public void perform(RepositoryConnection conn)
                    throws RepositoryException, MetadataRepositoryException {
                URIImpl userURI = new URIImpl(user.getUserURI().toString());
                URIImpl hasGraphURI = new URIImpl(hasGraphPredicate.toString());
                URIImpl contextURI = new URIImpl(graph.toString());
                URIImpl catalogURI = new URIImpl(QAC_CATALOG_GRAPH.toString());

                conn.add(userURI, hasGraphURI, contextURI, catalogURI);
                conn.add(rdf.asStatements(), contextURI);
            }
        });
    }

    public void setConnectionPool(SesameConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public synchronized SesameConnectionPool getConnectionPool() {
        if (this.connectionPool == null) {
            this.connectionPool = SesameConnectionPool.instance();
        }
        return this.connectionPool;
    }
}
