package com.example.edge;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.SessionScoped;

@RegisterAiService
@SessionScoped
public interface EdgeLLMService {
    
    String infer(@UserMessage String prompt);
}