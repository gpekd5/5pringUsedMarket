package com.example.fivespringusedmarket.product.entity;

import com.example.fivespringusedmarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품에 첨부된 이미지 정보를 저장하는 JPA 엔티티다.
 *
 * <p>Private S3 Bucket 정책을 유지하기 위해 이미지 Public URL이 아니라 S3 Object Key를 저장한다.
 * API 응답에서는 이 key를 그대로 노출하지 않고 Presigned URL로 변환한다.</p>
 */
@Getter
@Entity
@Table(
        name = "product_images",
        indexes = {
                @Index(name = "idx_product_images_product_id_sort_order", columnList = "product_id, sort_order")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 예: products/{uuid}.png 형태의 S3 Object Key를 저장한다.
    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Column(nullable = false)
    private int sortOrder;

    private ProductImage(Product product, String imageKey, int sortOrder) {
        this.product = product;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }

    /**
     * 요청으로 전달된 imageKey와 배열 순서를 기준으로 상품 이미지 엔티티를 생성한다.
     */
    public static ProductImage create(Product product, String imageKey, int sortOrder) {
        return new ProductImage(product, imageKey, sortOrder);
    }
}
