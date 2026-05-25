package com.baedal.support;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OrderMockService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        LocalDateTime now = LocalDateTime.now();

        Order o1234 = new Order(
                "2024-1234", "BHC치킨 역삼점",
                List.of(new OrderItem("허니콤보", 1, 18000), new OrderItem("콜라 1.25L", 1, 2000)),
                20000, now.minusMinutes(30), now.plusMinutes(15),
                "서울시 강남구 테헤란로 123", "역삼역 사거리", OrderStatus.DELIVERING);

        Order o1235 = new Order(
                "2024-1235", "맥도날드 강남점",
                List.of(new OrderItem("빅맥 세트", 2, 9500)),
                19000, now.minusMinutes(2), now.plusMinutes(40),
                "서울시 강남구 강남대로 456", null, OrderStatus.CREATED);

        Order o1236 = new Order(
                "2024-1236", "피자헛 서초점",
                List.of(new OrderItem("페퍼로니 피자 L", 1, 25000)),
                25000, now.minusHours(2), now.minusHours(1),
                "서울시 서초구 서초대로 789", null, OrderStatus.DELIVERED);

        Order o1237 = new Order(
                "2024-1237", "롯데리아 잠실점",
                List.of(new OrderItem("불고기버거 세트", 1, 7500)),
                7500, now.minusMinutes(15), now.plusMinutes(20),
                "서울시 송파구 올림픽로 101", null, OrderStatus.COOKING);

        Order o1238 = new Order(
                "2024-1238", "버거킹 홍대점",
                List.of(new OrderItem("와퍼 세트", 1, 9000)),
                9000, now.minusHours(3), now.minusHours(2),
                "서울시 마포구 홍대입구 202", null, OrderStatus.CREATED);
        o1238.cancel("고객 요청", now.minusHours(2).plusMinutes(5));

        Order o1239 = new Order(
                "2024-1239", "이디야커피 선릉점",
                List.of(new OrderItem("아이스 아메리카노", 2, 3500)),
                7000, now.minusMinutes(5), now.plusMinutes(35),
                "서울시 강남구 선릉로 303", null, OrderStatus.ACCEPTED);

        orders.put(o1234.getOrderId(), o1234);
        orders.put(o1235.getOrderId(), o1235);
        orders.put(o1236.getOrderId(), o1236);
        orders.put(o1237.getOrderId(), o1237);
        orders.put(o1238.getOrderId(), o1238);
        orders.put(o1239.getOrderId(), o1239);

        log.info("OrderMockService seeded — {}건", orders.size());
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
