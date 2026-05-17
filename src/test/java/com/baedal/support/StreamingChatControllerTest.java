package com.baedal.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StreamingChatController.class)
class StreamingChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatClient.Builder builder;

    @BeforeEach
    void setUp() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);

        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("배달", " 현황을", " 확인하겠습니다."));
    }

    @Test
    void chatStream_returns_text_event_stream() throws Exception {
        mockMvc.perform(post("/api/v1/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"주문번호 2024-1234 배달 어디쯤에 있어요?\"}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void chatStream_uses_system_prompt() throws Exception {
        mockMvc.perform(post("/api/v1/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"테스트\"}"))
            .andExpect(status().isOk());

        org.mockito.Mockito.verify(builder).defaultSystem(BaedalPrompt.SYSTEM_PROMPT);
    }
}