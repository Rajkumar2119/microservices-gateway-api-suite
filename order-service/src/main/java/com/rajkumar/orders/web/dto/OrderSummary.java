package com.rajkumar.orders.web.dto;

import com.rajkumar.orders.domain.OrderStatus;
import com.rajkumar.orders.domain.PurchaseOrder;
import java.math.BigDecimal;
import java.time.Instant;

/** List view: deliberately has no items, so listing orders never touches the order_item table. */
public record OrderSummary(String orderNumber, String customer, OrderStatus status, BigDecimal totalAmount,
                           Instant createdAt) {

    public static OrderSummary from(PurchaseOrder order) {
        return new OrderSummary(order.getOrderNumber(), order.getCustomerUsername(), order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt());
    }
}
