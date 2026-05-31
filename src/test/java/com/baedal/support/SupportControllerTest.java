package com.baedal.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupportController.class)
class SupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatClient.Builder builder;

    @MockBean
    private PerformanceLoggingAdvisor performanceAdvisor;

    @MockBean
    private OrderTools orderTools;

    /**
     * SupportController는 생성자에서 ChatClient를 한 번만 build한다.
     * @WebMvcTest는 @BeforeEach보다 먼저 빈을 초기화하므로
     * @TestConfiguration의 @Bean에서 mock 체인을 미리 구성한다.
     * RETURNS_SELF가 builder 체이닝(defaultSystem/defaultAdvisors/defaultTools)을 자동 처리한다.
     */
    @TestConfiguration
    static class Config {
        static ChatClient.CallResponseSpec callSpec;

        @Bean
        ChatClient.Builder mockChatClientBuilder() {
            ChatClient chatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            callSpec = mock(ChatClient.CallResponseSpec.class);

            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callSpec);

            ChatClient.Builder builder = mock(ChatClient.Builder.class, Answers.RETURNS_SELF);
            when(builder.build()).thenReturn(chatClient);
            return builder;
        }
    }

    @BeforeEach
    void setUp() {
        reset(Config.callSpec);
        when(Config.callSpec.entity(SupportResponse.class)).thenReturn(new SupportResponse(
                "배달 현황을 조회하겠습니다.",
                SupportResponse.Category.DELIVERY,
                SupportResponse.Urgency.NORMAL,
                "주문 현황 페이지를 확인하세요.",
                List.of("주문번호"),
                10,
                SupportResponse.Actionability.NEEDS_INFO
        ));
    }

    @Test
    void triage_returns_200_with_support_response() throws Exception {
        mockMvc.perform(post("/api/v1/support")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"주문번호 2024-1234 배달 어디쯤에 있어요?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").value("DELIVERY"))
            .andExpect(jsonPath("$.urgency").value("NORMAL"))
            .andExpect(jsonPath("$.estimatedResolutionMinutes").value(10));
    }

    @Test
    void triage_uses_system_prompt() throws Exception {
        verify(builder).defaultSystem(BaedalPrompt.SYSTEM_PROMPT);
    }

    @Test
    void triage_registers_performance_logging_advisor() throws Exception {
        verify(builder).defaultAdvisors(performanceAdvisor);
    }

    @Test
    void triage_registers_order_tools_in_constructor() throws Exception {
        verify(builder).defaultTools(orderTools);
    }

    @Test
    void triage_returns_503_when_llm_call_fails() throws Exception {
        reset(Config.callSpec);
        when(Config.callSpec.entity(SupportResponse.class))
                .thenThrow(new RuntimeException("LLM unavailable"));

        mockMvc.perform(post("/api/v1/support")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"주문번호 2024-1234 배달 어디쯤에 있어요?\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }
}