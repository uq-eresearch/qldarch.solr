package net.qldarch.ingest.transcript;

import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;
import net.qldarch.ingest.IngestStageFactory;

public class TranscriptDeployFactory implements IngestStageFactory<TranscriptDeploy> {
    public String getStageName() {
        return "deploy";
    }

    public TranscriptDeploy createIngestStage(Configuration configuration) {
        return new TranscriptDeploy(configuration);
    }
}
