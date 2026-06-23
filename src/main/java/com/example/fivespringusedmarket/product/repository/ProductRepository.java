package com.example.fivespringusedmarket.product.repository;

import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p FROM Product p
            WHERE (:category IS NULL OR p.category = :category)
              AND (:status IS NULL OR p.status = :status)
              AND (:keyword IS NULL OR p.title LIKE %:keyword%)
              AND (:sellerId IS NULL OR p.seller.id = :sellerId)
            """)
    Page<Product> searchProducts(
            @Param("category") ProductCategory category,
            @Param("keyword") String keyword,
            @Param("status") ProductStatus status,
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );
}
