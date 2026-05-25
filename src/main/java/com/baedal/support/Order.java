package com.baedal.support;

import java.time.LocalDateTime;
import java.util.List;

public class Order {

    private final String orderId;
    private final String storeName;
    private final List<OrderItem> items;
    private final int totalAmount;
    private final LocalDateTime orderedAt;
    private final LocalDateTime estimatedDeliveryAt;
    private final String deliveryAddress;
    private final String riderLocation;

    private OrderStatus status;
    private String canceledReason;
    private LocalDateTime canceledAt;

    public Order(String orderId, String storeName, List<OrderItem> items, int totalAmount,
                 LocalDateTime orderedAt, LocalDateTime estimatedDeliveryAt,
                 String deliveryAddress, String riderLocation, OrderStatus status) {
        this.orderId = orderId;
        this.storeName = storeName;
        this.items = items;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.estimatedDeliveryAt = estimatedDeliveryAt;
        this.deliveryAddress = deliveryAddress;
        this.riderLocation = riderLocation;
        this.status = status;
    }

    public void cancel(String reason, LocalDateTime at) {
        this.status = OrderStatus.CANCELED;
        this.canceledReason = reason;
        this.canceledAt = at;
    }

    public boolean isCancelable() {
        return status == OrderStatus.CREATED || status == OrderStatus.ACCEPTED;
    }

    public String getOrderId()                      { return orderId; }
    public String getStoreName()                    { return storeName; }
    public List<OrderItem> getItems()               { return items; }
    public int getTotalAmount()                     { return totalAmount; }
    public LocalDateTime getOrderedAt()             { return orderedAt; }
    public LocalDateTime getEstimatedDeliveryAt()   { return estimatedDeliveryAt; }
    public String getDeliveryAddress()              { return deliveryAddress; }
    public String getRiderLocation()                { return riderLocation; }
    public OrderStatus getStatus()                  { return status; }
    public String getCanceledReason()               { return canceledReason; }
    public LocalDateTime getCanceledAt()            { return canceledAt; }
}
