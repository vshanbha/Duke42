package com.example.edge;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface EdgeToolLLMService {    

    @SystemMessage("""
        You are an AI assistant that helps users and you have access to various tools to assist in answering questions.
        When using a tool, always convert the tool's response into a natural, human-readable answer.
        If the user's question is unclear, politely ask for clarification. 
        It's ok to use tools to answer questions if needed otherwise, answer it directly using your own knowledge.
        Always communicate in a friendly and professional manner, and ensure your responses are easy to understand.
    """)    
    @McpToolBox("default")
    String chatWithTools(@MemoryId String chatId, @UserMessage String message);
}