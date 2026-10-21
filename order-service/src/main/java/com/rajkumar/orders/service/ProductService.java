package com.rajkumar.orders.service;

import com.rajkumar.orders.config.CacheConfig;
import com.rajkumar.orders.domain.Product;
import com.rajkumar.orders.repository.ProductRepository;
import com.rajkumar.orders.web.dto.ProductRequest;
import com.rajkumar.orders.web.dto.ProductResponse;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    /** Active catalogue, served from Redis after the first call until it is changed or the TTL expires. */
    @Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "'active'")
    @Transactional(readOnly = true)
    public List<ProductResponse> activeCatalogue() {
        return products.findByActiveTrueOrderByNameAsc().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse bySku(String sku) {
        return products.findBySku(sku).map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("Product " + sku + " not found"));
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, allEntries = true)
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (products.existsBySku(request.sku())) {
            throw new BusinessRuleException("Product " + request.sku() + " already exists");
        }
        Product product = new Product(request.sku(), request.name(), request.priceGbp());
        if (Boolean.FALSE.equals(request.active())) {
            product.update(request.name(), request.priceGbp(), false);
        }
        return ProductResponse.from(products.save(product));
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, allEntries = true)
    @Transactional
    public ProductResponse update(String sku, ProductRequest request) {
        Product product = products.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Product " + sku + " not found"));
        product.update(request.name(), request.priceGbp(), request.active() == null || request.active());
        return ProductResponse.from(product);
    }
}
