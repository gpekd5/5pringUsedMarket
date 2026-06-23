package com.example.fivespringusedmarket.product.repository;

import com.example.fivespringusedmarket.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
