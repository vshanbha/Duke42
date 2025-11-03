package com.example.edge;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface EdgeLLMService {
    
    String infer(@UserMessage String prompt);
}