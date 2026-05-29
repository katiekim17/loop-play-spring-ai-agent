package com.baedal.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderMockService orderService;

    @Tool(description = """
            주어진 주문번호의 상세 정보를 조회한다.
            고객이 메뉴, 수량, 금액, 주문 상태, 예상 배달 완료 시각을 물을 때 호출한다.
            배달 위치나 라이더 정보를 물을 때는 이 Tool을 호출하지 않는다.
            주문번호는 "YYYY-XXXX" 형식이며 (예: 2024-1234),
            존재하지 않으면 null을 반환한다.
            """)
    public OrderDetailView getOrderDetail(
            @ToolParam(description = "조회할 주문번호. 예: 2024-1234") String orderId) {
        log.info("[Tool] getOrderDetail(orderId={})", orderId);
        return orderService.findById(orderId).map(this::toDetailView).orElse(null);
    }

    @Tool(description = """
            주어진 주문번호의 현재 배달 상태와 라이더 위치를 조회한다.
            고객이 배달 위치, 라이더가 어디쯤 있는지, 배달이 언제 오는지를 물을 때 호출한다.
            메뉴나 금액을 물을 때는 이 Tool을 호출하지 않는다.
            배달 중(DELIVERING)인 주문에 대해서만 riderLocation이 반환되며,
            아직 배달이 시작되지 않았거나 이미 완료된 주문은 riderLocation이 null이다.
            존재하지 않는 주문번호면 null을 반환한다.
            """)
    public DeliveryStatusView getDeliveryStatus(
            @ToolParam(description = "배달 상태를 조회할 주문번호. 예: 2024-1234") String orderId) {
        log.info("[Tool] getDeliveryStatus(orderId={})", orderId);
        return orderService.findById(orderId).map(this::toDeliveryView).orElse(null);
    }

    @Tool(description = "Cancel an order. Call this immediately when the customer requests cancellation. Do not judge eligibility yourself — this tool handles all cases and returns CANCELED, ALREADY_CANCELED, NOT_CANCELABLE, or NOT_FOUND.")
    public CancelOrderResult cancelOrder(
            @ToolParam(description = "Order ID to cancel, e.g. 2024-1234") String orderId,
            @ToolParam(description = "Cancellation reason from the customer. Use '고객 요청' if not stated.") String reason) {
        log.info("[Tool] cancelOrder(orderId={}, reason={})", orderId, reason);

        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            return new CancelOrderResult(orderId, CancelOrderResult.Outcome.NOT_FOUND,
                    "해당 주문번호를 찾을 수 없습니다.");
        }
        if (order.getStatus() == OrderStatus.CANCELED) {
            return new CancelOrderResult(orderId, CancelOrderResult.Outcome.ALREADY_CANCELED,
                    "해당 주문은 이미 취소된 상태입니다. (취소 사유: " + order.getCanceledReason() + ")");
        }
        if (!order.isCancelable()) {
            return new CancelOrderResult(orderId, CancelOrderResult.Outcome.NOT_CANCELABLE,
                    "조리가 이미 시작되어(" + order.getStatus() + ") 자동 취소가 불가합니다. 상담원 연결이 필요합니다.");
        }
        order.cancel(reason, LocalDateTime.now());
        return new CancelOrderResult(orderId, CancelOrderResult.Outcome.CANCELED,
                "주문이 취소되었습니다. 결제 취소는 카드사에 따라 최대 7영업일이 소요될 수 있습니다.");
    }

    private OrderDetailView toDetailView(Order order) {
        return new OrderDetailView(
                order.getOrderId(),
                order.getStoreName(),
                order.getItems().stream()
                        .map(i -> new OrderDetailView.Line(i.menuName(), i.quantity(), i.unitPrice()))
                        .toList(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getOrderedAt(),
                order.getEstimatedDeliveryAt()
        );
    }

    private DeliveryStatusView toDeliveryView(Order order) {
        return new DeliveryStatusView(
                order.getOrderId(),
                order.getStatus().name(),
                order.getRiderLocation(),
                order.getEstimatedDeliveryAt()
        );
    }
}
