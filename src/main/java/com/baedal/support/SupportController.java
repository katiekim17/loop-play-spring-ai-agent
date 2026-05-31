package com.baedal.support;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/support")
public class SupportController {

    private final ChatClient chatClient;

    public SupportController(ChatClient.Builder builder,
                             PerformanceLoggingAdvisor performanceAdvisor,
                             OrderTools orderTools) {
        this.chatClient = builder
                .defaultSystem(BaedalPrompt.SYSTEM_PROMPT)
                .defaultAdvisors(performanceAdvisor)
                .defaultTools(orderTools)
                .build();
    }

    @PostMapping
    public SupportResponse triage(@Valid @RequestBody ChatRequest req) {
        return chatClient.prompt()
                .user(req.message())
                .call()
                .entity(SupportResponse.class);
    }
}