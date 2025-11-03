package com.example.edge;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;


@QuarkusTest
public class EdgeResourceTest {

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
        System.out.println(response);
    }

}
