package com.example.cliai.rag;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/**
 * BLUEPRINT Step 10: ETL pipeline – TextReader → TokenTextSplitter → EmbeddingModel → VectorStore.
 *
 * Reads a text document (default: TUTORIAL.md), splits it into token-sized chunks and
 * embeds them into the PgVector store. Run once via {@code --rag.enabled=true
 * --rag.ingest.on-startup=true} or programmatically through {@link #ingest()}.
 */
@Component
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
class IngestionService implements ApplicationRunner {

    private final VectorStore vectorStore;
    private final String sourcePath;
    private final boolean ingestOnStartup;

    IngestionService(VectorStore vectorStore,
                     @Value("${rag.ingest.source:TUTORIAL.md}") String sourcePath,
                     @Value("${rag.ingest.on-startup:false}") boolean ingestOnStartup) {
        this.vectorStore = vectorStore;
        this.sourcePath = sourcePath;
        this.ingestOnStartup = ingestOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (ingestOnStartup) {
            ingest();
        }
    }

    /** ETL: read → split → embed+store. Returns the number of chunks written. */
    int ingest() {
        TextReader textReader = new TextReader(new FileSystemResource(sourcePath));
        textReader.getCustomMetadata().put("source", sourcePath);

        List<Document> chunks = new TokenTextSplitter().apply(textReader.get());
        vectorStore.add(chunks);
        System.out.printf("[RAG] Ingested %d chunk(s) from %s into the vector store%n", chunks.size(), sourcePath);
        return chunks.size();
    }
}
