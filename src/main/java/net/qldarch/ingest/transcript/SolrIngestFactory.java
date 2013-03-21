package net.qldarch.ingest.transcript;

import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;
import net.qldarch.ingest.IngestStageFactory;

public class SolrIngestFactory implements IngestStageFactory<SolrIngest> {
    public String getStageName() {
        return "solringest";
    }

    public SolrIngest createIngestStage(Configuration configuration) {
        return new SolrIngest(configuration);
    }
}
