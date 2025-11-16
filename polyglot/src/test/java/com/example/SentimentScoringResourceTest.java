package com.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SentimentScoringResourceTest {
    @Test
    void testNeutralSentiment() {
        String result = given()
            .contentType(MediaType.TEXT_PLAIN)
            .body("This is a test sentence for sentiment analysis.")
            .when()
            .post("/polyglot/score")
            .then()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .extract().body().asString();
             
        assertTrue(result.length() > 0, "The result should not be empty");
        System.out.println("Result: " + result);
        assertTrue(result.toLowerCase().contains("\"classification\": \"neutral\""), "The result contains a valid sentiment");
    }

    @Test
    void testPositiveSentiment() {
        String result = given()
            .contentType(MediaType.TEXT_PLAIN)
            .body("This is a wonderful and amazing product! I am very happy.")
            .when()
            .post("/polyglot/score")
            .then()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .extract().body().asString();
             
        assertTrue(result.length() > 0, "The result should not be empty");
        System.out.println("Result: " + result);
        assertTrue(result.toLowerCase().contains("\"classification\": \"positive\""), "The result contains a valid positive sentiment");
    }

    @Test
    void testNegativeSentiment() {
        String result = given()
            .contentType(MediaType.TEXT_PLAIN)
            .body("I hate this, it is a terrible experience.")
            .when()
            .post("/polyglot/score")
            .then()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .extract().body().asString();
             
        assertTrue(result.length() > 0, "The result should not be empty");
        System.out.println("Result: " + result);
        assertTrue(result.toLowerCase().contains("\"classification\": \"negative\""), "The result contains a valid negative sentiment");
    }

}