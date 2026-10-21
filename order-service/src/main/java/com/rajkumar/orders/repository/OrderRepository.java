package com.rajkumar.orders.repository;

import com.rajkumar.orders.domain.OrderStatus;
import com.rajkumar.orders.domain.PurchaseOrder;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /** Loads items in the same query (no N+1). */
    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findByIdempotencyKey(String idempotencyKey);

    /** Served by index idx_orders_customer_created (customer_username, created_at). */
    Page<PurchaseOrder> findByCustomerUsername(String customerUsername, Pageable pageable);

    /** Served by index idx_orders_status_created (status, created_at). */
    Page<PurchaseOrder> findByStatus(OrderStatus status, Pageable pageable);
}
