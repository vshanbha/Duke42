package com.example.edge;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;

@QuarkusTest
public class EdgeResourceTest {

    private UUID chatId;

    @BeforeEach
    public void setup() {
        // Generate a new UUID before each test to ensure isolation
        chatId = UUID.randomUUID();
    }

    @Test
    public void testInferEndpoint() throws IOException, InterruptedException {
        String prompt = "Tell me a joke. prefix your response with the exact word 'Birbal'";

        String response = given()
                .body(prompt)
                .contentType("text/plain")
                .when()
                .post("/edge/infer")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .extract()
                .asString();

        // Assert that the response body is not empty and contains some content
        assertTrue(response != null && !response.isEmpty(), "Response body should not be empty");
        System.out.println("testInferEndpoint: " + response);
    }

    @Test
    public void testChatEndpoint() {
        String message = "Hello, how are you?";

        given()
                .queryParam("message", message)
                .contentType("text/plain")
                .post("/edge/chat/" + chatId)
                .then()
                .statusCode(200)
                .contentType("text/plain");
    }

    @Test
    public void testChatMemory() {

        String message1 = "My name is Duke42";
        String message2 = "What is my name?";

        String response1 = given()
                .queryParam("message", message1)
                .contentType("text/plain")
                .post("/edge/chat/" + chatId)
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .extract()
                .asString();
        System.out.println("testChatMemory 1: " + response1);
        String response2 = given()
                .queryParam("message", message2)
                .contentType("text/plain")
                .post("/edge/chat/" + chatId)
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .extract()
                .asString();
        // Assert that the response body is not empty and contains some content
        assertTrue(response2 != null && !response2.isEmpty(), "Response body should not be empty");
        System.out.println("testChatMemory 2: " + response2);
        assertTrue(response2.toLowerCase().contains("duke42"), "Forgot my name");
    }

    @Test
    public void testChatHistoryOutsideMessageWindow() {

        String message1 = "My name is Duke42";
        String messageX = "One line of text - Hitchhikers Guide To Galaxy Trivia";
        String messageN = "What is my name?";

        String response1 = given()
                .queryParam("message", message1)
                .contentType("text/plain")
                .post("/edge/chat/" + chatId)
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .extract()
                .asString();
        System.out.println("testChatMemory 1: " + response1);

        for (int i = 2; i < 7; i++) {
            String response = given()
                    .queryParam("message", messageX)
                    .contentType("text/plain")
                    .post("/edge/chat/" + chatId)
                    .then()
                    .statusCode(200)
                    .contentType("text/plain")
                    .extract()
                    .asString();
            System.out.println("testChatMemory " + i + ": " + response);

        }

        String responseN = given()
                .queryParam("message", messageN)
                .contentType("text/plain")
                .post("/edge/chat/" + chatId)
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .extract()
                .asString();
        // Assert that the response body is not empty and contains some content
        assertTrue(responseN != null && !responseN.isEmpty(), "Response body should not be empty");
        System.out.println("testChatMemory N: " + responseN);
        assertFalse(responseN.toLowerCase().contains("duke42"), "Remembers my name");
    }

    @Test
    public void testEmptyUserId() {
        given()
                .queryParam("message", "test message")
                .contentType("text/plain")
                .post("/edge/chat/")
                .then()
                .statusCode(404);
    }

}
