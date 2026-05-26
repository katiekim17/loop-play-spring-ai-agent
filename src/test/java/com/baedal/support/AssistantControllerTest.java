package com.baedal.support;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatClient.Builder builder;

    @MockBean
    private PerformanceLoggingAdvisor performanceAdvisor;

    @MockBean
    private OrderTools orderTools;

    /**
     * AssistantController uses the constructor pattern (builds ChatClient once at startup).
     * @WebMvcTest initializes the bean BEFORE @BeforeEach, so stubs must be wired here,
     * before context creation. RETURNS_SELF handles all builder chaining; only build() needs stubbing.
     */
    @TestConfiguration
    static class Config {
        @Bean
        ChatClient.Builder mockChatClientBuilder() {
            ChatClient chatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callSpec);
            when(callSpec.content()).thenReturn("배달 현황 확인 중입니다.");

            ChatClient.Builder builder = mock(ChatClient.Builder.class, Answers.RETURNS_SELF);
            when(builder.build()).thenReturn(chatClient);

            return builder;
        }
    }

    @Test
    void ask_returns_200_with_string_response() throws Exception {
        mockMvc.perform(post("/api/v1/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"주문번호 2024-1234 어디쯤이에요?\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void ask_registers_order_tools_in_constructor() throws Exception {
        mockMvc.perform(post("/api/v1/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"테스트\"}"))
                .andExpect(status().isOk());

        verify(builder).defaultTools(orderTools);
    }

    @Test
    void ask_uses_system_prompt() throws Exception {
        mockMvc.perform(post("/api/v1/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"테스트\"}"))
                .andExpect(status().isOk());

        verify(builder).defaultSystem(BaedalPrompt.SYSTEM_PROMPT);
    }

    @Test
    void ask_returns_400_when_message_is_blank() throws Exception {
        mockMvc.perform(post("/api/v1/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ask_returns_400_when_message_exceeds_1000_chars() throws Exception {
        mockMvc.perform(post("/api/v1/assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"" + "A".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
