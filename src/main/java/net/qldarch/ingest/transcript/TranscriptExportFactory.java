package net.qldarch.ingest.transcript;

import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;
import net.qldarch.ingest.IngestStageFactory;

public class TranscriptExportFactory implements IngestStageFactory {
    public String getStageName() {
        return "transcript";
    }

    public IngestStage createIngestStage(Configuration configuration) {
        return new TranscriptExport(configuration);
    }
}
