package com.rajkumar.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        assertThat(OrderStatus.CREATED.canMoveTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canMoveTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canMoveTo(OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    void cannotSkipStepsOrLeaveFinalStates() {
        assertThat(OrderStatus.CREATED.canMoveTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.SHIPPED.canMoveTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.DELIVERED.allowedNext()).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedNext()).isEmpty();
    }
}
