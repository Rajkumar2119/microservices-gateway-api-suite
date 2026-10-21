package com.rajkumar.orders.web.dto;

import com.rajkumar.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(@NotNull OrderStatus status) {
}
