package com.baedal.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class SupportResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_include_estimatedResolutionMinutes_field() throws Exception {
        var response = new SupportResponse(
            "테스트 요약",
            SupportResponse.Category.DELIVERY,
            SupportResponse.Urgency.NORMAL,
            "배달 현황을 확인하세요.",
            List.of("주문번호"),
            10
        );

        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("estimatedResolutionMinutes");
        assertThat(response.estimatedResolutionMinutes()).isEqualTo(10);
    }

    @Test
    void should_have_complaint_category() {
        assertThat(SupportResponse.Category.valueOf("COMPLAINT"))
            .isEqualTo(SupportResponse.Category.COMPLAINT);
    }

    @Test
    void should_deserialize_from_json() throws Exception {
        String json = """
            {
              "summary": "라이더 사고 접수",
              "category": "COMPLAINT",
              "urgency": "HIGH",
              "nextAction": "보상팀에 전달합니다.",
              "neededInfo": ["주문번호", "사진"],
              "estimatedResolutionMinutes": 2880
            }
            """;

        SupportResponse response = mapper.readValue(json, SupportResponse.class);

        assertThat(response.category()).isEqualTo(SupportResponse.Category.COMPLAINT);
        assertThat(response.estimatedResolutionMinutes()).isEqualTo(2880);
    }
}