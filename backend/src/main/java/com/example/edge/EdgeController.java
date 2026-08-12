package com.example.edge;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/edge")
@Tag(name = "Duke42 AI Agent", description = "REST API for Spring AI agent with tools and memory")
class EdgeController {

    private final ChatClients chatClients;

    EdgeController(ChatClients chatClients) {
        this.chatClients = chatClients;
    }

    @PostMapping(value = "/infer", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Single-shot inference", description = "Send a prompt and get a response without conversation memory")
    public String infer(
            @Parameter(description = "The prompt to send to the LLM") @RequestBody String prompt) {
        return chatClients.infer(prompt);
    }

    @PostMapping(value = "/chat/{chatId}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Chat with memory", description = "Send a message with conversation history (memory per chatId)")
    public String chat(
            @Parameter(description = "Unique chat session ID") @PathVariable String chatId,
            @Parameter(description = "The message to send") @RequestParam String message) {
        return chatClients.chat(chatId, message);
    }

    @PostMapping(value = "/toolChat/{chatId}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Chat with MCP tools", description = "Chat with access to MCP tools (e.g., sentiment analysis from polyglot module)")
    public String toolChat(
            @Parameter(description = "Unique chat session ID") @PathVariable String chatId,
            @Parameter(description = "The message to send") @RequestParam String message) {
        return chatClients.toolChat(chatId, message);
    }
}
