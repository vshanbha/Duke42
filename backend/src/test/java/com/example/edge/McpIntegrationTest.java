package com.example.edge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for MCP tool calling via polyglot sentiment server.
 * Skipped unless polyglot MCP is running on port 9000.
 *
 * Run manually:
 *   cd polyglot && mvn package -DskipTests && java -jar target/polyglot-runner.jar
 *   cd backend && mvn test -Dtest=McpIntegrationTest -Dmcp.integration=true
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.ai.mcp.client.enabled=true",
    "spring.ai.mcp.client.sse.connections.polyglot.url=http://localhost:9000"
})
@EnabledIfSystemProperty(named = "mcp.integration", matches = "true")
class McpIntegrationTest {

    @Autowired
    private ChatClients chatClients;

    @Test
    void shouldReachPolyglotMcpServer() throws Exception {
        assumeTrue(isPortOpen(9000), "Polyglot MCP server not running on port 9000");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:9000/mcp"))
            .GET()
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isLessThan(500);
    }

    @Test
    void shouldUseSentimentToolViaToolChat() {
        assumeTrue(isPortOpen(9000), "Polyglot MCP server not running on port 9000");

        String response = chatClients.toolChat("mcp-test",
            "Analyze the sentiment of this text: I love Spring AI!");

        assertThat(response).isNotBlank();
        assertThat(response.toLowerCase()).doesNotContain("server not ready");
    }

    private static boolean isPortOpen(int port) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI("http://localhost:" + port).toURL().openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setRequestMethod("GET");
            conn.connect();
            conn.getResponseCode();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
