package com.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PolyglotScoringResourceTest {
    @Test
    void testScoreEndpoint() {
        String result = given()
            .contentType(MediaType.TEXT_PLAIN)
            .body("This is a test sentence for sentiment analysis.")
            .when()
            .post("/polyglot/sentiment")
            .then()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .extract().body().asString();
             
        assertTrue(result.length() > 0, "The result should not be empty");
        System.out.println("Result: " + result);
        assertTrue(result.toLowerCase().contains("\"classification\": \"neutral\""), "The result contains a valid sentiment");
    }

    @Test
    void testAnomalyDetectionEndpoint() {
        String transactionRecord = "{\"TransactionAmount\": 50.0, \"CustomerAge\": 30, \"TransactionDuration\": 120, \"LoginAttempts\": 1, \"AccountBalance\": 1000.0}";
        
        String result = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(transactionRecord)
            .when()
            .post("/polyglot/anomaly")
            .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().body().asString();
        
        assertTrue(result.length() > 0, "The result should not be empty");
        System.out.println("Anomaly Detection Result: " + result);
        assertTrue(result.contains("\"score\"") || result.contains("\"classification\""), "The result should contain anomaly_score or classification field");
    }

}