package com.baedal.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMockServiceTest {

    private OrderMockService service;

    @BeforeEach
    void setUp() {
        service = new OrderMockService();
        service.seed();
    }

    @Test
    void seed_creates_all_6_orders() {
        assertThat(service.findById("2024-1234")).isPresent();
        assertThat(service.findById("2024-1235")).isPresent();
        assertThat(service.findById("2024-1236")).isPresent();
        assertThat(service.findById("2024-1237")).isPresent();
        assertThat(service.findById("2024-1238")).isPresent();
        assertThat(service.findById("2024-1239")).isPresent();
    }

    @Test
    void order_2024_1234_is_DELIVERING_with_riderLocation() {
        Order o = service.findById("2024-1234").orElseThrow();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        assertThat(o.getRiderLocation()).isEqualTo("역삼역 사거리");
    }

    @Test
    void order_2024_1238_is_pre_canceled_with_reason() {
        Order o = service.findById("2024-1238").orElseThrow();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(o.getCanceledReason()).isNotBlank();
        assertThat(o.getCanceledAt()).isNotNull();
    }

    @Test
    void findById_returns_empty_for_unknown_orderId() {
        assertThat(service.findById("9999-0000")).isEmpty();
    }
}
