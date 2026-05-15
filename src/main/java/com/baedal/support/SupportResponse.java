package com.baedal.support;

import java.util.List;

public record SupportResponse(
        String summary,
        Category category,
        Urgency urgency,
        String nextAction,
        List<String> neededInfo,
        Integer estimatedResolutionMinutes
) {
    public enum Category { ORDER, DELIVERY, REFUND, PAYMENT, COMPLAINT, ETC }
    public enum Urgency  { LOW, NORMAL, HIGH, CRITICAL }
}
