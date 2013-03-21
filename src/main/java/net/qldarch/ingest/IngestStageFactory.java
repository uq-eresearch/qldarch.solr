package net.qldarch.ingest;

public interface IngestStageFactory<T extends IngestStage> {
    public String getStageName();
    public T createIngestStage(Configuration configuration);
}
