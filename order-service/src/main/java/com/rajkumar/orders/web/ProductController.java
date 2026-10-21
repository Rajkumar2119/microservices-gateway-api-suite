package com.rajkumar.orders.web;

import com.rajkumar.orders.service.ProductService;
import com.rajkumar.orders.web.dto.ProductRequest;
import com.rajkumar.orders.web.dto.ProductResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> catalogue() {
        return productService.activeCatalogue();
    }

    @GetMapping("/{sku}")
    public ProductResponse product(@PathVariable String sku) {
        return productService.bySku(sku);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{sku}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable String sku, @Valid @RequestBody ProductRequest request) {
        return productService.update(sku, request);
    }
}
