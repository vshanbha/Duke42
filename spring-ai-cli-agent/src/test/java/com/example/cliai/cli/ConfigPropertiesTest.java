package com.example.cliai.cli;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the test application.properties shadowing the main one: if the
 * ollama model is not bound here, @SpringBootTest-based integration tests fall back
 * to Ollama's hardcoded "mistral" default and fail.
 */
@SpringBootTest
class ConfigPropertiesTest {

    @Autowired
    Environment environment;

    @Test
    void ollamaModelIsBoundFromTestProfile() {
        String model = environment.getProperty("spring.ai.ollama.chat.model");
        assertThat(model).isNotNull().isEqualTo("gemma4:e4b-mlx");
    }
}
