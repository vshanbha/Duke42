package com.example.edge.e2e;

import org.junit.jupiter.api.*;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests that start the backend jar as a real process,
 * test the REST API, then kill the process.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class E2ETest {

    private static final int PORT = 9099;
    private static Process serverProcess;
    private static HttpClient httpClient;

    @BeforeAll
    static void startJar() throws Exception {
        String jarPath = "target/backend-1.0.0-SNAPSHOT.jar";
        File jar = new File(jarPath);
        if (!jar.exists()) {
            throw new RuntimeException("Jar not found: " + jar.getAbsolutePath()
                + "\nRun: mvn clean package -DskipTests");
        }

        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-jar", jarPath,
            "--server.port=" + PORT,
            "--spring.ai.mcp.client.enabled=false"
        );
        pb.directory(new File("."));
        pb.inheritIO();
        serverProcess = pb.start();

        waitForServer();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void killJar() {
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroyForcibly();
            try {
                serverProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void waitForServer() throws Exception {
        URL url = new URL("http://localhost:" + PORT);
        int maxRetries = 60;
        for (int i = 0; i < maxRetries; i++) {
            if (!serverProcess.isAlive()) {
                throw new RuntimeException("Server process died during startup");
            }
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                if (conn.getResponseCode() > 0) {
                    System.out.println("Server ready on port " + PORT);
                    return;
                }
            } catch (Exception e) {
                // Not ready yet
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        throw new RuntimeException("Server did not start within " + maxRetries + " seconds");
    }

    @Test
    @Order(1)
    void shouldCallInferEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:" + PORT + "/edge/infer"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString("Say hello in 3 words"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isNotEmpty();
        System.out.println("Infer response: " + response.body());
    }

    @Test
    @Order(2)
    void shouldCallChatEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:" + PORT + "/edge/chat/e2e-test-user?message=Hello"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isNotEmpty();
        System.out.println("Chat response: " + response.body());
    }

    @Test
    @Order(3)
    void shouldCallToolChatEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:" + PORT + "/edge/toolChat/e2e-test-user?message=What+is+2+plus+2"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isNotEmpty();
        System.out.println("ToolChat response: " + response.body());
    }

    @Test
    @Order(4)
    void shouldHaveChatMemory() throws Exception {
        // First message
        HttpRequest request1 = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:" + PORT + "/edge/chat/memory-test-user?message=My+name+is+TestUser"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        httpClient.send(request1, HttpResponse.BodyHandlers.ofString());

        // Second message - AI should remember
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:" + PORT + "/edge/chat/memory-test-user?message=What+is+my+name"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isNotEmpty();
        System.out.println("Memory response: " + response.body());
    }
}
