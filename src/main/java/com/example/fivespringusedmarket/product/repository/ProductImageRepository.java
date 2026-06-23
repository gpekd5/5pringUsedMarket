package com.example.fivespringusedmarket.product.repository;

import com.example.fivespringusedmarket.product.entity.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // 대표 이미지 일괄 조회 시 사용한다.
    List<ProductImage> findByProductIdInAndSortOrder(List<Long> productIds, int sortOrder);

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    void deleteByProductId(Long productId);
}
