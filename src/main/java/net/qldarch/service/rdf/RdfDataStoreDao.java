package net.qldarch.service.rdf;

import java.net.URI;

public interface RdfDataStoreDao {
    public void performInsert(final RdfDescription rdf, final User user,
            final URI hasGraphPredicate, final URI graph) throws MetadataRepositoryException;
}
