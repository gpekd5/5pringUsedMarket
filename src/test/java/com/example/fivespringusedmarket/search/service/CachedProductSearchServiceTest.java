package com.example.fivespringusedmarket.search.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import com.example.fivespringusedmarket.search.repository.ProductSearchRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cached-product-search-service-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=caffeine",
        "jwt.secret=12345678901234567890123456789012",
        "jwt.access-token-expiration=3600000"
})
class CachedProductSearchServiceTest {

    @Autowired
    private CachedProductSearchReader cachedProductSearchReader;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductSearchRepository productSearchRepository;

    @BeforeEach
    void setUp() {
        // 테스트 간 캐시 데이터가 섞이지 않도록 productSearch 캐시를 비운다.
        Cache productSearchCache = cacheManager.getCache("productSearch");
        if (productSearchCache != null) {
            productSearchCache.clear();
        }
    }

    @Test
    @DisplayName("캐시 검색 - 같은 검색 조건으로 두 번 조회하면 Repository는 한 번만 호출된다")
    void sameConditionUsesCache() {
        // given
        ProductSearchCondition condition = new ProductSearchCondition(
                "아이폰",
                null,
                null,
                null
        );

        PageRequest pageable = PageRequest.of(0, 10);

        when(productSearchRepository.search(any(ProductSearchCondition.class), eq(pageable)))
                .thenReturn(new PageImpl<ProductListItemResponse>(List.of()));

        // when
        cachedProductSearchReader.search(condition, pageable);
        cachedProductSearchReader.search(condition, pageable);

        // then
        // 같은 조건의 두 번째 요청은 캐시 Hit가 발생하므로 Repository는 한 번만 호출된다.
        verify(productSearchRepository, times(1))
                .search(any(ProductSearchCondition.class), eq(pageable));
    }

    @Test
    @DisplayName("캐시 검색 - 검색어가 다르면 캐시 Key가 달라져 Repository가 각각 호출된다")
    void differentKeywordDoesNotUseSameCache() {
        // given
        ProductSearchCondition iphoneCondition = new ProductSearchCondition(
                "아이폰",
                null,
                null,
                null
        );

        ProductSearchCondition galaxyCondition = new ProductSearchCondition(
                "갤럭시",
                null,
                null,
                null
        );

        PageRequest pageable = PageRequest.of(0, 10);

        when(productSearchRepository.search(any(ProductSearchCondition.class), eq(pageable)))
                .thenReturn(new PageImpl<ProductListItemResponse>(List.of()));

        // when
        cachedProductSearchReader.search(iphoneCondition, pageable);
        cachedProductSearchReader.search(galaxyCondition, pageable);

        // then
        // 검색어가 다르면 캐시 Key도 다르므로 Repository가 두 번 호출된다.
        verify(productSearchRepository, times(2))
                .search(any(ProductSearchCondition.class), eq(pageable));
    }

    @Test
    @DisplayName("캐시 검색 - 페이지가 다르면 캐시 Key가 달라져 Repository가 각각 호출된다")
    void differentPageDoesNotUseSameCache() {
        // given
        ProductSearchCondition condition = new ProductSearchCondition(
                "아이폰",
                null,
                null,
                null
        );

        PageRequest firstPage = PageRequest.of(0, 10);
        PageRequest secondPage = PageRequest.of(1, 10);

        when(productSearchRepository.search(any(ProductSearchCondition.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<ProductListItemResponse>(List.of()));

        // when
        cachedProductSearchReader.search(condition, firstPage);
        cachedProductSearchReader.search(condition, secondPage);

        // then
        // 페이지가 다르면 검색 결과도 달라질 수 있으므로 Repository가 각각 호출된다.
        verify(productSearchRepository, times(1))
                .search(any(ProductSearchCondition.class), eq(firstPage));

        verify(productSearchRepository, times(1))
                .search(any(ProductSearchCondition.class), eq(secondPage));
    }
}