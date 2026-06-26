package com.example.fivespringusedmarket.wish.repository;

import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.wish.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long ProductId);

    Optional<Wish> findByMemberIdAndProductId(Long memberId, Long productId);

    long countByMemberIdAndProductStatusNot(Long memberId, ProductStatus status);
}
