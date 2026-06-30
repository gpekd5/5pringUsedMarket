package com.example.fivespringusedmarket.search.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import com.example.fivespringusedmarket.search.dto.ProductSearchSortType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class SearchCacheKeyGeneratorTest {

    private final SearchCacheKeyGenerator generator = new SearchCacheKeyGenerator();

    @Test
    @DisplayName("같은 검색 조건과 같은 페이지 정보이면 같은 캐시 key를 생성한다")
    void sameConditionAndPageableCreatesSameKey() {
        // given
        ProductSearchCondition condition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        String key1 = generator.generate(condition, pageable);
        String key2 = generator.generate(condition, pageable);

        // then
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("검색어가 다르면 다른 캐시 key를 생성한다")
    void differentKeywordCreatesDifferentKey() {
        // given
        ProductSearchCondition iphoneCondition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        ProductSearchCondition galaxyCondition = new ProductSearchCondition(
                "갤럭시",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        String iphoneKey = generator.generate(iphoneCondition, pageable);
        String galaxyKey = generator.generate(galaxyCondition, pageable);

        // then
        assertThat(iphoneKey).isNotEqualTo(galaxyKey);
    }

    @Test
    @DisplayName("카테고리가 다르면 다른 캐시 key를 생성한다")
    void differentCategoryCreatesDifferentKey() {
        // given
        ProductSearchCondition digitalCondition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        ProductSearchCondition furnitureCondition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.FURNITURE,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        String digitalKey = generator.generate(digitalCondition, pageable);
        String furnitureKey = generator.generate(furnitureCondition, pageable);

        // then
        assertThat(digitalKey).isNotEqualTo(furnitureKey);
    }

    @Test
    @DisplayName("상태가 다르면 다른 캐시 key를 생성한다")
    void differentStatusCreatesDifferentKey() {
        // given
        ProductSearchCondition onSaleCondition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        ProductSearchCondition soldCondition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.SOLD,
                ProductSearchSortType.LATEST
        );

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        String onSaleKey = generator.generate(onSaleCondition, pageable);
        String soldKey = generator.generate(soldCondition, pageable);

        // then
        assertThat(onSaleKey).isNotEqualTo(soldKey);
    }

    @Test
    @DisplayName("정렬 조건이 다르면 다른 캐시 key를 생성한다")
    void differentSortTypeCreatesDifferentKey() {
        // given
        ProductSearchCondition latestCondition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        ProductSearchCondition priceAscCondition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.PRICE_ASC
        );

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        String latestKey = generator.generate(latestCondition, pageable);
        String priceAscKey = generator.generate(priceAscCondition, pageable);

        // then
        assertThat(latestKey).isNotEqualTo(priceAscKey);
    }

    @Test
    @DisplayName("페이지 번호가 다르면 다른 캐시 key를 생성한다")
    void differentPageCreatesDifferentKey() {
        // given
        ProductSearchCondition condition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        PageRequest firstPage = PageRequest.of(0, 10);
        PageRequest secondPage = PageRequest.of(1, 10);

        // when
        String firstKey = generator.generate(condition, firstPage);
        String secondKey = generator.generate(condition, secondPage);

        // then
        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    @DisplayName("페이지 크기가 다르면 다른 캐시 key를 생성한다")
    void differentSizeCreatesDifferentKey() {
        // given
        ProductSearchCondition condition = new ProductSearchCondition(
                "아이폰",
                ProductCategory.DIGITAL,
                ProductStatus.ON_SALE,
                ProductSearchSortType.LATEST
        );

        PageRequest size10 = PageRequest.of(0, 10);
        PageRequest size20 = PageRequest.of(0, 20);

        // when
        String size10Key = generator.generate(condition, size10);
        String size20Key = generator.generate(condition, size20);

        // then
        assertThat(size10Key).isNotEqualTo(size20Key);
    }
}