package com.example.edge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EdgeControllerTest {

    @MockitoBean
    private ChatClients chatClients;

    @Autowired
    private EdgeController edgeController;

    @Test
    void contextLoads() {
        assertThat(edgeController).isNotNull();
    }

    @Test
    void shouldHaveChatClientsInjected() {
        assertThat(chatClients).isNotNull();
    }

    @Test
    void shouldExposeRagChatEndpoint() throws Exception {
        org.springframework.test.web.servlet.MockMvc mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
            .standaloneSetup(edgeController).build();
        org.mockito.Mockito.when(chatClients.chat("rag-1", "hello")).thenReturn("hi");

        // RAG endpoint falls back to plain chat when no QuestionAnswerAdvisor bean exists (test context has none)
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/edge/ragChat/rag-1?message=hello")
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("hi")));
    }
}
