package com.rajkumar.orders.repository;

import com.rajkumar.orders.domain.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findBySkuIn(Collection<String> skus);

    List<Product> findByActiveTrueOrderByNameAsc();

    boolean existsBySku(String sku);
}
