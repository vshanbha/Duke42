package com.example.edge;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/edge")
@Tag(name = "Duke42 AI Agent", description = "REST API for Spring AI agent with tools and memory")
class EdgeController {

    private final ChatClient chatClient;
    private final ChatClient chatClientWithMemory;

    EdgeController(ChatClient chatClient, ChatClient chatClientWithMemory) {
        this.chatClient = chatClient;
        this.chatClientWithMemory = chatClientWithMemory;
    }

    @PostMapping(value = "/infer", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Single-shot inference", description = "Send a prompt and get a response without conversation memory")
    public String infer(
            @Parameter(description = "The prompt to send to the LLM") @RequestBody String prompt) {
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    @PostMapping(value = "/chat/{chatId}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Chat with memory", description = "Send a message with conversation history (memory per chatId)")
    public String chat(
            @Parameter(description = "Unique chat session ID") @PathVariable String chatId,
            @Parameter(description = "The message to send") @RequestParam String message) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return "User ID cannot be empty.";
        }

        return chatClientWithMemory.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
            .call()
            .content();
    }

    @PostMapping(value = "/toolChat/{chatId}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Chat with MCP tools", description = "Chat with access to MCP tools (e.g., sentiment analysis from polyglot module)")
    public String toolChat(
            @Parameter(description = "Unique chat session ID") @PathVariable String chatId,
            @Parameter(description = "The message to send") @RequestParam String message) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return "User ID cannot be empty.";
        }

        return chatClientWithMemory.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId + "-tools"))
            .call()
            .content();
    }
}
