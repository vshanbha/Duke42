package com.example.edge;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * BLUEPRINT project structure: root {@code ChatService} with chat + chatStream + ragChat.
 *
 * chat/chatStream delegate to the UI-facing service ({@code com.example.edge.ui.ChatService});
 * ragChat adds retrieval-augmented generation when a RAG stack is configured
 * (QuestionAnswerAdvisor present) and falls back to plain memory-backed chat otherwise –
 * same optional-dependency pattern as MCP.
 */
@Service
public class ChatService {

    private static final System.Logger LOG = System.getLogger(ChatService.class.getName());

    private final com.example.edge.ui.ChatService uiChatService;
    private final ChatClients chatClients;
    private final ObjectProvider<QuestionAnswerAdvisor> ragAdvisor;

    ChatService(com.example.edge.ui.ChatService uiChatService, ChatClients chatClients,
                ObjectProvider<QuestionAnswerAdvisor> ragAdvisor) {
        this.uiChatService = uiChatService;
        this.chatClients = chatClients;
        this.ragAdvisor = ragAdvisor;
    }

    public String chat(String chatId, String message) {
        return uiChatService.chat(chatId, message);
    }

    public Flux<String> chatStream(String chatId, String message) {
        return uiChatService.chatStream(chatId, message);
    }

    /** RAG chat with graceful fallback when no VectorStore/QuestionAnswerAdvisor is configured. */
    public String ragChat(String chatId, String message) {
        QuestionAnswerAdvisor advisor = ragAdvisor.getIfAvailable();
        if (advisor == null) {
            LOG.log(System.Logger.Level.INFO,
                "RAG not configured (no VectorStore) - falling back to plain chat for chatId={0}", chatId);
            return uiChatService.chat(chatId, message);
        }
        return chatClients.ragChat(chatId, message, advisor);
    }
}
