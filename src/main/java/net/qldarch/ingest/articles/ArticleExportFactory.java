package net.qldarch.ingest.articles;

import net.qldarch.ingest.Configuration;
import net.qldarch.ingest.IngestStage;
import net.qldarch.ingest.IngestStageFactory;

public class ArticleExportFactory implements IngestStageFactory<ArticleExport> {
    public String getStageName() {
        return "article";
    }

    public ArticleExport createIngestStage(Configuration configuration) {
        return new ArticleExport(configuration);
    }
}
