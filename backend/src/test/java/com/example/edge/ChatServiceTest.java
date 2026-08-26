package com.example.edge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** BLUEPRINT project structure: root ChatService (chat + chatStream + ragChat) with RAG fallback. */
class ChatServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<QuestionAnswerAdvisor> ragAdvisor = mock(ObjectProvider.class);
    private final com.example.edge.ui.ChatService uiChatService = mock(com.example.edge.ui.ChatService.class);
    private final ChatClients chatClients = mock(ChatClients.class);
    private final ChatService chatService = new ChatService(uiChatService, chatClients, ragAdvisor);

    @Test
    void chatShouldDelegateToUiChatService() {
        when(uiChatService.chat("c1", "hi")).thenReturn("hello");

        assertThat(chatService.chat("c1", "hi")).isEqualTo("hello");
        verifyNoInteractions(chatClients);
    }

    @Test
    void ragChatShouldFallBackToPlainChatWhenRagNotConfigured() {
        when(ragAdvisor.getIfAvailable()).thenReturn(null);
        when(uiChatService.chat("c1", "question")).thenReturn("plain answer");

        assertThat(chatService.ragChat("c1", "question")).isEqualTo("plain answer");
        verifyNoInteractions(chatClients);
    }

    @Test
    void ragChatShouldUseQuestionAnswerAdvisorWhenConfigured() {
        QuestionAnswerAdvisor advisor = mock(QuestionAnswerAdvisor.class);
        when(ragAdvisor.getIfAvailable()).thenReturn(advisor);
        when(chatClients.ragChat(eq("c1"), eq("question"), any(QuestionAnswerAdvisor.class)))
            .thenReturn("rag answer");

        assertThat(chatService.ragChat("c1", "question")).isEqualTo("rag answer");
        verify(chatClients).ragChat("c1", "question", advisor);
    }
}
