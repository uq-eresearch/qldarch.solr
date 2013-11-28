package net.qldarch.service.rdf;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public interface RepositoryOperation {
    public void perform(RepositoryConnection conn)
        throws RepositoryException, MetadataRepositoryException;
}
