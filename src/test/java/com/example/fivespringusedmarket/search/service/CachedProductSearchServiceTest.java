package com.example.fivespringusedmarket.search.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.config.CacheConfig;
import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import com.example.fivespringusedmarket.search.repository.ProductSearchRepository;
import com.example.fivespringusedmarket.search.cache.SearchCacheKeyGenerator;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {
        CachedProductSearchReader.class,
        CacheConfig.class,
        SearchCacheKeyGenerator.class
})
class CachedProductSearchReaderTest {

    @Autowired
    private CachedProductSearchReader cachedProductSearchReader;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductSearchRepository productSearchRepository;

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache("productSearchV2");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("Caffeine 캐시 검색 - 같은 검색 조건으로 두 번 조회하면 Repository는 한 번만 호출된다")
    void sameConditionUsesCaffeineCache() {
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
        cachedProductSearchReader.searchWithCaffeine(condition, pageable);
        cachedProductSearchReader.searchWithCaffeine(condition, pageable);

        // then
        verify(productSearchRepository, times(1))
                .search(any(ProductSearchCondition.class), eq(pageable));
    }

    @Test
    @DisplayName("Caffeine 캐시 검색 - 검색어가 다르면 캐시 Key가 달라져 Repository가 각각 호출된다")
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
        cachedProductSearchReader.searchWithCaffeine(iphoneCondition, pageable);
        cachedProductSearchReader.searchWithCaffeine(galaxyCondition, pageable);

        // then
        verify(productSearchRepository, times(2))
                .search(any(ProductSearchCondition.class), eq(pageable));
    }

    @Test
    @DisplayName("Caffeine 캐시 검색 - 페이지가 다르면 캐시 Key가 달라져 Repository가 각각 호출된다")
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

        when(productSearchRepository.search(any(ProductSearchCondition.class), any(Pageable.class)))
                .thenReturn(new PageImpl<ProductListItemResponse>(List.of()));

        // when
        cachedProductSearchReader.searchWithCaffeine(condition, firstPage);
        cachedProductSearchReader.searchWithCaffeine(condition, secondPage);

        // then
        verify(productSearchRepository, times(1))
                .search(any(ProductSearchCondition.class), eq(firstPage));

        verify(productSearchRepository, times(1))
                .search(any(ProductSearchCondition.class), eq(secondPage));
    }
}