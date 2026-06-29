package com.example.fivespringusedmarket.wish.entity;

import com.example.fivespringusedmarket.common.entity.BaseEntity;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 관심 등록한 상품을 나타내는 Entity입니다.
 */
@Getter
@Entity
@Table(name = "wishes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wishes_member_product", columnNames = {"member_id", "product_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wish extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Wish(Member member, Product product) {
        this.member = member;
        this.product = product;
    }

    public static Wish create(Member member, Product product) {
        return new Wish(member, product);
    }


}
