package com.example.cliai.rag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** BLUEPRINT Step 10 ETL: TextReader → TokenTextSplitter → VectorStore.add */
class IngestionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void ingestShouldSplitAndStoreDocumentChunks() throws Exception {
        Path source = tempDir.resolve("TUTORIAL.md");
        String paragraph = "The AskUserQuestionTool lets the agent ask the user a clarifying question. ".repeat(40);
        Files.writeString(source, "# Tutorial\n\n" + paragraph + "\n");

        VectorStore vectorStore = mock(VectorStore.class);
        IngestionService service = new IngestionService(vectorStore, source.toString(), false);

        int chunks = service.ingest();

        assertThat(chunks).isGreaterThan(0);
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<Document>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue()).allSatisfy(doc ->
            assertThat(doc.getText()).doesNotContain("\0"));
    }

    @Test
    void runnerShouldSkipIngestWhenNotConfigured() {
        VectorStore vectorStore = mock(VectorStore.class);
        IngestionService service = new IngestionService(vectorStore, "does-not-exist.md", false);

        service.run(null);

        org.mockito.Mockito.verifyNoInteractions(vectorStore);
    }
}
