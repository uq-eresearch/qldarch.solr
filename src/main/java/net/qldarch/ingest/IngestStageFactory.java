package net.qldarch.ingest;

public interface IngestStageFactory {
    public String getStageName();
    public IngestStage createIngestStage(Configuration configuration);
}
