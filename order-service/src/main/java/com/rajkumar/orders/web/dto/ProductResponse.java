package com.rajkumar.orders.web.dto;

import com.rajkumar.orders.domain.Product;
import java.io.Serializable;
import java.math.BigDecimal;

/** Serializable because the catalogue is stored in the Redis cache. */
public record ProductResponse(String sku, String name, BigDecimal priceGbp, boolean active) implements Serializable {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getSku(), product.getName(), product.getPriceGbp(), product.isActive());
    }
}
