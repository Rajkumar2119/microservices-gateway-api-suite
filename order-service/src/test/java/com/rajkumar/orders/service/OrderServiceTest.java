package com.rajkumar.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rajkumar.orders.domain.OrderStatus;
import com.rajkumar.orders.domain.Product;
import com.rajkumar.orders.domain.PurchaseOrder;
import com.rajkumar.orders.repository.OrderRepository;
import com.rajkumar.orders.repository.ProductRepository;
import com.rajkumar.orders.web.dto.CreateOrderRequest;
import com.rajkumar.orders.web.dto.OrderLineRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private ProductRepository products;

    @InjectMocks
    private OrderService orderService;

    private final Product keyboard = new Product("KB-1", "Keyboard", new BigDecimal("89.99"));
    private final Product mouse = new Product("MS-1", "Mouse", new BigDecimal("24.50"));

    @Test
    void createsOrderWithServerSidePricesAndMergedLines() {
        when(products.findBySkuIn(anyCollection())).thenReturn(List.of(keyboard, mouse));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderService.CreatedOrder result = orderService.create("alice", new CreateOrderRequest(List.of(
                new OrderLineRequest("kb-1", 1),
                new OrderLineRequest("MS-1", 1),
                new OrderLineRequest("KB-1", 1))), null);

        assertThat(result.created()).isTrue();
        assertThat(result.order().customer()).isEqualTo("alice");
        assertThat(result.order().status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.order().items()).hasSize(2);
        assertThat(result.order().items().get(0).quantity()).isEqualTo(2);
        assertThat(result.order().totalAmount()).isEqualByComparingTo("204.48");
        assertThat(result.order().orderNumber()).matches("ORD-\\d{8}-[0-9A-F]{8}");
    }

    @Test
    void rejectsUnknownAndInactiveProducts() {
        Product retired = new Product("OLD-1", "Retired", new BigDecimal("1.00"));
        retired.update("Retired", new BigDecimal("1.00"), false);
        when(products.findBySkuIn(anyCollection())).thenReturn(List.of(retired));

        assertThatThrownBy(() -> orderService.create("alice", new CreateOrderRequest(List.of(
                new OrderLineRequest("OLD-1", 1),
                new OrderLineRequest("NOPE-1", 1))), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("OLD-1")
                .hasMessageContaining("NOPE-1");
        verify(orders, never()).save(any());
    }

    @Test
    void replayedIdempotencyKeyReturnsOriginalOrder() {
        PurchaseOrder existing = new PurchaseOrder("ORD-20261021-ABCDEF12", "alice", "key-1");
        existing.addItem(keyboard, 1);
        when(orders.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        OrderService.CreatedOrder result = orderService.create("alice",
                new CreateOrderRequest(List.of(new OrderLineRequest("KB-1", 5))), "key-1");

        assertThat(result.created()).isFalse();
        assertThat(result.order().orderNumber()).isEqualTo("ORD-20261021-ABCDEF12");
        verify(orders, never()).save(any());
    }

    @Test
    void customersCannotSeeOtherCustomersOrders() {
        PurchaseOrder bobs = new PurchaseOrder("ORD-1", "bob", null);
        when(orders.findByOrderNumber("ORD-1")).thenReturn(Optional.of(bobs));

        assertThatThrownBy(() -> orderService.get("ORD-1", "alice", false))
                .isInstanceOf(NotFoundException.class);
        assertThat(orderService.get("ORD-1", "admin", true).customer()).isEqualTo("bob");
    }

    @Test
    void invalidStatusChangeIsABusinessRuleError() {
        when(orders.findByOrderNumber("ORD-2")).thenReturn(Optional.of(new PurchaseOrder("ORD-2", "alice", null)));

        assertThatThrownBy(() -> orderService.changeStatus("ORD-2", OrderStatus.DELIVERED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CREATED to DELIVERED");
    }
}
