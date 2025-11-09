package com.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SentimentScoringResourceTest {
    @Test
    void testScoreEndpoint() {
        String result = given()
            .contentType(MediaType.TEXT_PLAIN)
            .body("This is a test sentence for sentiment analysis.")
            .when()
            .post("/polygot/score")
            .then()
                .statusCode(200)
                .extract().body().as(String.class);        
             
        assertTrue(result.length() > 0, "The result should not be empty");
    }

}