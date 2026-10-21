package com.rajkumar.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "price_gbp", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceGbp;

    @Column(nullable = false)
    private boolean active = true;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal priceGbp) {
        this.sku = sku;
        this.name = name;
        this.priceGbp = priceGbp;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceGbp() {
        return priceGbp;
    }

    public boolean isActive() {
        return active;
    }

    public void update(String name, BigDecimal priceGbp, boolean active) {
        this.name = name;
        this.priceGbp = priceGbp;
        this.active = active;
    }
}
