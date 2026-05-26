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

    private final ChatClient.Builder builder;
    private final PerformanceLoggingAdvisor performanceAdvisor;
    private final OrderTools orderTools;

    public SupportController(ChatClient.Builder builder,
                             PerformanceLoggingAdvisor performanceAdvisor,
                             OrderTools orderTools) {
        this.builder = builder;
        this.performanceAdvisor = performanceAdvisor;
        this.orderTools = orderTools;
    }

    @PostMapping
    public SupportResponse triage(@Valid @RequestBody ChatRequest req) {
        return builder
                .defaultSystem(BaedalPrompt.SYSTEM_PROMPT)
                .defaultAdvisors(performanceAdvisor)
                .build()
                .prompt()
                .user(req.message())
                .tools(orderTools)
                .call()
                .entity(SupportResponse.class);
    }
}