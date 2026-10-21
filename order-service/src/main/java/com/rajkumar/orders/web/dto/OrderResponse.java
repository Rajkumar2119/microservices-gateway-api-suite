package com.rajkumar.orders.web.dto;

import com.rajkumar.orders.domain.OrderItem;
import com.rajkumar.orders.domain.OrderStatus;
import com.rajkumar.orders.domain.PurchaseOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderNumber,
        String customer,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        List<Line> items) {

    public record Line(String sku, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

        static Line from(OrderItem item) {
            return new Line(item.getSku(), item.getProductName(), item.getQuantity(), item.getUnitPrice(),
                    item.lineTotal());
        }
    }

    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(order.getOrderNumber(), order.getCustomerUsername(), order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt(), order.getUpdatedAt(),
                order.getItems().stream().map(Line::from).toList());
    }
}
