package com.example.edge;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EdgeLLMServiceImpl implements EdgeLLMService {
    @Override
    public String infer(String prompt) {
        // Replace with real Ollama or local LLM invocation later.
        return "Mock response for prompt: " + prompt;
    }
}
