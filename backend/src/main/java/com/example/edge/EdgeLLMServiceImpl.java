package com.example.edge;

import java.time.Duration;

import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.model.output.Response;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EdgeLLMServiceImpl implements EdgeLLMService {

    private final OllamaLanguageModel model;

    public EdgeLLMServiceImpl() {
        this.model = OllamaLanguageModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("smollm2:135m")
                .timeout(Duration.ofSeconds(180000))
                .build();
    }

    @Override
    public String infer(String prompt) {
        try {
            Response<String> response = model.generate(prompt);
            return response.content();
        } catch (Exception e) {
            System.err.println("Error during inference: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
