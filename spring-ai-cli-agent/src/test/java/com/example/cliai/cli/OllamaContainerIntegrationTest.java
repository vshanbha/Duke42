package com.example.cliai.cli;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BLUEPRINT Step 15: ChatClient integration backed by a real {@link OllamaContainer}
 * via {@code @ServiceConnection} (spring-ai-spring-boot-testcontainers +
 * org.testcontainers:ollama).
 *
 * Fully opt-in so default {@code mvn test} stays dependency-free:
 * run with {@code mvn test -Dtc.ollama=true} and a running Docker daemon.
 * This smoke test verifies the container wiring (context boot + /api/tags);
 * real-model evaluations remain available via {@code mvn test -Devals=true}.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named = "tc.ollama", matches = "true")
class OllamaContainerIntegrationTest {

    @Container
    @ServiceConnection
    static final OllamaContainer OLLAMA = new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"))
        .waitingFor(Wait.forHttp("/").forStatusCode(200));

    @MockitoBean
    ChatLoop chatLoop;

    @Autowired
    ChatClient chatClient;

    @Test
    void springAiShouldConnectToOllamaContainer() throws Exception {
        assertThat(chatClient).isNotNull();

        HttpRequest request = HttpRequest.newBuilder(URI.create(OLLAMA.getEndpoint() + "/api/tags")).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("models");
    }
}
