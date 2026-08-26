package com.example.cliai.rag;

import javax.sql.DataSource;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * BLUEPRINT Step 10: RAG + Vector Databases.
 *
 * ETL lives in {@link IngestionService}; retrieval is a {@link QuestionAnswerAdvisor}
 * (the "RAG as Advisor" pattern – similarity search against the vector store) that
 * {@code AgentConfiguration} adds to the ChatClient when present.
 *
 * Gated behind {@code rag.enabled=true} (like the MCP client) so default runs and
 * tests need no Postgres. Wiring is manual – library only, no starter auto-config:
 * {@code PgVectorStore} + {@code OllamaEmbeddingModel} per blueprint.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
class RagConfiguration {

    @Bean
    DataSource ragDataSource(@Value("${rag.datasource.url}") String url,
                             @Value("${rag.datasource.username:postgres}") String username,
                             @Value("${rag.datasource.password:postgres}") String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    @Bean
    JdbcTemplate ragJdbcTemplate(DataSource ragDataSource) {
        return new JdbcTemplate(ragDataSource);
    }

    @Bean
    VectorStore vectorStore(JdbcTemplate ragJdbcTemplate,
                            EmbeddingModel embeddingModel,
                            @Value("${rag.vectorstore.dimensions:768}") int dimensions) {
        return PgVectorStore.builder(ragJdbcTemplate, embeddingModel)
            .dimensions(dimensions)
            .initializeSchema(true)
            .build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore).build();
    }
}
