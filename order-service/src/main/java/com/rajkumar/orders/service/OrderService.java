package com.rajkumar.orders.service;

import com.rajkumar.orders.domain.OrderStatus;
import com.rajkumar.orders.domain.Product;
import com.rajkumar.orders.domain.PurchaseOrder;
import com.rajkumar.orders.repository.OrderRepository;
import com.rajkumar.orders.repository.ProductRepository;
import com.rajkumar.orders.web.dto.CreateOrderRequest;
import com.rajkumar.orders.web.dto.OrderLineRequest;
import com.rajkumar.orders.web.dto.OrderResponse;
import com.rajkumar.orders.web.dto.OrderSummary;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderRepository orders;
    private final ProductRepository products;

    public OrderService(OrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    /**
     * Places an order for the calling user. Prices always come from the database, never the client.
     * Sending the same Idempotency-Key twice returns the original order instead of creating a duplicate.
     */
    @Transactional
    public CreatedOrder create(String username, CreateOrderRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<PurchaseOrder> existing = orders.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                PurchaseOrder order = existing.get();
                if (!order.getCustomerUsername().equals(username)) {
                    throw new BusinessRuleException("Idempotency-Key already used");
                }
                return new CreatedOrder(OrderResponse.from(order), false);
            }
        }

        // Merge repeated SKUs into one line each.
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest line : request.items()) {
            quantities.merge(line.sku().trim().toUpperCase(Locale.ROOT), line.quantity(), Integer::sum);
        }

        Map<String, Product> found = products.findBySkuIn(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getSku, Function.identity()));
        List<String> unavailable = quantities.keySet().stream()
                .filter(sku -> !found.containsKey(sku) || !found.get(sku).isActive())
                .toList();
        if (!unavailable.isEmpty()) {
            throw new BusinessRuleException("Unknown or inactive products: " + unavailable);
        }

        PurchaseOrder order = new PurchaseOrder(newOrderNumber(), username, idempotencyKey);
        quantities.forEach((sku, qty) -> order.addItem(found.get(sku), qty));
        return new CreatedOrder(OrderResponse.from(orders.save(order)), true);
    }

    /** Customers see only their own orders; for anyone else's the answer is 404, not 403, so order numbers can't be probed. */
    @Transactional(readOnly = true)
    public OrderResponse get(String orderNumber, String username, boolean admin) {
        PurchaseOrder order = orders.findByOrderNumber(orderNumber)
                .filter(o -> admin || o.getCustomerUsername().equals(username))
                .orElseThrow(() -> new NotFoundException("Order " + orderNumber + " not found"));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> myOrders(String username, Pageable pageable) {
        return orders.findByCustomerUsername(username, pageable).map(OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> byStatus(OrderStatus status, Pageable pageable) {
        return orders.findByStatus(status, pageable).map(OrderSummary::from);
    }

    @Transactional
    public OrderResponse changeStatus(String orderNumber, OrderStatus next) {
        PurchaseOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order " + orderNumber + " not found"));
        try {
            order.moveTo(next);
        } catch (IllegalStateException e) {
            throw new BusinessRuleException(e.getMessage());
        }
        return OrderResponse.from(order);
    }

    /** Customers may cancel their own order while it has not shipped. */
    @Transactional
    public OrderResponse cancel(String orderNumber, String username) {
        PurchaseOrder order = orders.findByOrderNumber(orderNumber)
                .filter(o -> o.getCustomerUsername().equals(username))
                .orElseThrow(() -> new NotFoundException("Order " + orderNumber + " not found"));
        try {
            order.moveTo(OrderStatus.CANCELLED);
        } catch (IllegalStateException e) {
            throw new BusinessRuleException(e.getMessage());
        }
        return OrderResponse.from(order);
    }

    private static String newOrderNumber() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "ORD-" + LocalDate.now(ZoneOffset.UTC).format(ORDER_DATE) + "-" + random;
    }

    /** {@code created} is false when an Idempotency-Key replay returned an existing order. */
    public record CreatedOrder(OrderResponse order, boolean created) {
    }
}
