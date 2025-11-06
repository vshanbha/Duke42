package com.example.edge;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface EdgeLLMService {    
    
    String infer(@UserMessage String prompt);

    String chat(@MemoryId String chatId, @UserMessage String message);
}