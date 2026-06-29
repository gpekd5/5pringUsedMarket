package com.example.fivespringusedmarket.wish.repository;

import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.wish.dto.WishProductResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.fivespringusedmarket.product.entity.QProduct.product;
import static com.example.fivespringusedmarket.product.entity.QProductImage.productImage;
import static com.example.fivespringusedmarket.wish.entity.QWish.wish;

/**
 * 관심상품 목록 조회 QueryDSL Repository입니다.
 */
@Repository
@RequiredArgsConstructor
public class WishQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 로그인 사용자의 관심상품 목록을 최신 관심 등록순으로 조회합니다.
     *
     * <p>DELETED 상태의 상품은 제외하고,
     * SOLD 상태의 상품은 관심상품 기록으로 볼 수 있도록 포함합니다.</p>
     */
    public List<WishProductResponse> findMyWishes(Long memberId) {
        return queryFactory
                .select(Projections.constructor(
                        WishProductResponse.class,
                        product.id,
                        product.title,
                        product.price,
                        product.category,
                        product.status,
                        productImage.imageUrl,
                        wish.createdAt
                ))
                .from(wish)
                .join(wish.product, product)
                .leftJoin(productImage)
                .on(productImage.product.eq(product),
                        productImage.sortOrder.eq(0))
                .where(wish.member.id.eq(memberId),
                        product.status.ne(ProductStatus.DELETED))
                .orderBy(wish.createdAt.desc())
                .fetch();
    }
}
