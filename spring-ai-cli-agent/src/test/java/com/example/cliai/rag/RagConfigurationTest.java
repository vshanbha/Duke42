package com.example.cliai.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * BLUEPRINT Step 10: RAG wiring is gated behind rag.enabled=true (like the MCP client),
 * so default runs and tests need no Postgres.
 *
 * The happy-path test uses a real pgvector container because {@code PgVectorStore}
 * initializes its schema via {@code afterPropertiesSet()} at bean creation.
 * Fully opt-in: {@code mvn test -Dtc.pgvector=true} with a running Docker daemon.
 */
class RagConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(RagConfiguration.class)
        .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class));

    @Test
    void shouldWireNoBeansByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(VectorStore.class);
            assertThat(context).doesNotHaveBean(QuestionAnswerAdvisor.class);
            assertThat(context).doesNotHaveBean("ragDataSource");
        });
    }

    @Test
    @EnabledIfSystemProperty(named = "tc.pgvector", matches = "true")
    void shouldWirePgVectorStackWhenEnabled() {
        // PgVectorStore.afterPropertiesSet() issues DDL – needs a live pgvector instance.
        try (PostgreSQLContainer<?> pg = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))) {
            pg.start();
            runner.withPropertyValues(
                    "rag.enabled=true",
                    "rag.datasource.url=" + pg.getJdbcUrl(),
                    "rag.datasource.username=" + pg.getUsername(),
                    "rag.datasource.password=" + pg.getPassword(),
                    "rag.vectorstore.dimensions=768")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VectorStore.class);
                    assertThat(context.getBean(VectorStore.class)).isInstanceOf(PgVectorStore.class);
                    assertThat(context).hasSingleBean(QuestionAnswerAdvisor.class);
                    assertThat(context).hasBean("ragDataSource");
                    assertThat(context).hasBean("ragJdbcTemplate");
                });
        }
    }
}
