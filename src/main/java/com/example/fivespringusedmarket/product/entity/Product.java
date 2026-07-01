package com.example.fivespringusedmarket.product.entity;

import com.example.fivespringusedmarket.common.entity.BaseEntity;
import com.example.fivespringusedmarket.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 중고 상품 정보를 저장하는 JPA 엔티티다.
 */
@Getter
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_category_status_created_at", columnList = "category, status, created_at DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member seller;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    private Product(Member seller, String title, String description, int price, ProductCategory category) {
        this.seller = seller;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.status = ProductStatus.ON_SALE;
    }

    public static Product create(Member seller, String title, String description, int price, ProductCategory category) {
        return new Product(seller, title, description, price, category);
    }

    public void update(String title, Integer price, String description, ProductCategory category) {
        if (title != null) this.title = title;
        if (price != null) this.price = price;
        if (description != null) this.description = description;
        if (category != null) this.category = category;
    }

    public void updateStatus(ProductStatus status) {
        this.status = status;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.seller.getId().equals(memberId);
    }

    public boolean isSold() {
        return this.status == ProductStatus.SOLD;
    }

    public boolean isDeleted() {
        return this.status == ProductStatus.DELETED;
    }

    // ON_SALE→RESERVED, ON_SALE→SOLD, RESERVED→SOLD만 허용한다.
    public boolean canTransitionTo(ProductStatus next) {
        return switch (this.status) {
            case ON_SALE -> next == ProductStatus.RESERVED || next == ProductStatus.SOLD;
            case RESERVED -> next == ProductStatus.SOLD;
            default -> false;
        };
    }

}
