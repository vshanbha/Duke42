package com.example.edge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/edge")
class EdgeController {

    private final ChatClient chatClient;

    EdgeController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/infer", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String infer(@RequestBody String prompt) {
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    @PostMapping(value = "/chat/{chatId}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(
        @PathVariable String chatId,
        @RequestParam String message
    ) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return "User ID cannot be empty.";
        }

        return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
            .call()
            .content();
    }
}
