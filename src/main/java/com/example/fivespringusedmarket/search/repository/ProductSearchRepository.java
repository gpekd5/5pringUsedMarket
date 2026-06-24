package com.example.fivespringusedmarket.search.repository;

import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import com.example.fivespringusedmarket.search.dto.ProductSearchSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.fivespringusedmarket.product.entity.QProduct.product;
import static com.example.fivespringusedmarket.product.entity.QProductImage.productImage;

/**
 * 상품 검색 전용 Repository입니다.
 *
 * <p>QueryDSL을 사용하여 키워드, 카테고리, 판매 상태, 정렬 조건을
 * 동적으로 조합해 상품 목록을 조회합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final JPAQueryFactory queryFactory;

    // 상품 검색 조건과 페이징 정보 기반으로 상품 목록 조회
    public Page<ProductListItemResponse> search(ProductSearchCondition condition, Pageable pageable) {
        // 현재 페이지에 보여줄 상품 목록 조회
        List<ProductListItemResponse> content = queryFactory
                .select(Projections.constructor(ProductListItemResponse.class,
                        product.id,
                        product.seller.id,
                        product.title,
                        product.price,
                        product.category.stringValue(),
                        product.status.stringValue(),
                        productImage.imageUrl,
                        product.createdAt))
                .from(product)
                .leftJoin(productImage) // 상품과 상품 이미지 테이블 연결
                .on(
                        productImage.product.id.eq(product.id), // 해당 상품의 이미지만 조회
                        productImage.sortOrder.eq(0) // 여러 이미지 중 대표 이미지만 조회
                )
                .where(
                        notDeleted(),
                        keywordContains(condition.keyword()),
                        categoryEq(condition.category()),
                        statusEq(condition.status())
                )
                .orderBy(getOrderSpecifier(condition.sort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        //전체 검색 결과 개수 조회
        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        notDeleted(),
                        keywordContains(condition.keyword()),
                        categoryEq(condition.category()),
                        statusEq(condition.status())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 삭제된 상품을 검색 결과에서 제외
    private BooleanExpression notDeleted(){
        return product.status.ne(ProductStatus.DELETED);
    }

    // 상품명 또는 상품 설명에 검색어가 포함된 상품 조회
    private BooleanExpression keywordContains(String keyword){
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        return product.title.contains(keyword)
                .or(product.description.contains(keyword));
    }

    // 카테고리 조건이 있을 경우 해당 카테고리의 상품만 조회
    private BooleanExpression categoryEq(ProductCategory category) {
        if (category == null) {
            return null;
        }

        return product.category.eq(category);
    }

    // 판매 상태 조건이 있을 경우 해당 상태의 상품만 조회
    private BooleanExpression statusEq(ProductStatus status) {
        if (status == null) {
            return null;
        }

        return product.status.eq(status);
    }

    // 요청한 정렬 기준에 맞는 정렬 조건 반환
    private OrderSpecifier<?> getOrderSpecifier(ProductSearchSortType sort) {
        if (sort == ProductSearchSortType.OLDEST) {
            return product.createdAt.asc();
        }

        if (sort == ProductSearchSortType.PRICE_DESC) {
            return product.price.desc();
        }

        if (sort == ProductSearchSortType.PRICE_ASC) {
            return product.price.asc();
        }

        return product.createdAt.desc(); // 기본 정렬 : 최신 등록순
    }


}
