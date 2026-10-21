package com.rajkumar.orders.web;

import com.rajkumar.orders.domain.OrderStatus;
import com.rajkumar.orders.service.OrderService;
import com.rajkumar.orders.web.dto.CreateOrderRequest;
import com.rajkumar.orders.web.dto.OrderResponse;
import com.rajkumar.orders.web.dto.OrderSummary;
import com.rajkumar.orders.web.dto.PageResponse;
import com.rajkumar.orders.web.dto.StatusChangeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 201 for a new order; 200 with the original order when an Idempotency-Key is replayed. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(Authentication auth,
                                                @Valid @RequestBody CreateOrderRequest request,
                                                @RequestHeader(name = "Idempotency-Key", required = false)
                                                @Size(max = 64) String idempotencyKey) {
        OrderService.CreatedOrder result = orderService.create(auth.getName(), request, idempotencyKey);
        if (!result.created()) {
            return ResponseEntity.ok(result.order());
        }
        return ResponseEntity.created(URI.create("/orders/" + result.order().orderNumber())).body(result.order());
    }

    @GetMapping
    public PageResponse<OrderSummary> myOrders(Authentication auth,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(orderService.myOrders(auth.getName(), pageable));
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse get(Authentication auth, @PathVariable String orderNumber) {
        return orderService.get(orderNumber, auth.getName(), isAdmin(auth));
    }

    @PostMapping("/{orderNumber}/cancel")
    public OrderResponse cancel(Authentication auth, @PathVariable String orderNumber) {
        return orderService.cancel(orderNumber, auth.getName());
    }

    @PatchMapping("/{orderNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse changeStatus(@PathVariable String orderNumber,
                                      @Valid @RequestBody StatusChangeRequest request) {
        return orderService.changeStatus(orderNumber, request.status());
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<OrderSummary> byStatus(@RequestParam OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(orderService.byStatus(status, pageable));
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
