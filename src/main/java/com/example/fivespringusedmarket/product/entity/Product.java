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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
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

    public void updateStatus(ProductStatus status) {
        this.status = status;
    }
}
