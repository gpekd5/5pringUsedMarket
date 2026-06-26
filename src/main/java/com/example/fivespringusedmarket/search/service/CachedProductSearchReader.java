package com.example.fivespringusedmarket.search.service;

import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import com.example.fivespringusedmarket.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐시가 적용된 상품 검색 조회 전용 Service입니다.
 *
 * <p>검색 로그 저장과 인기검색어 집계는 SearchService에서 매번 수행하고,
 * 실제 상품 목록 조회에만 캐시를 적용하기 위해 별도 클래스로 분리했습니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CachedProductSearchReader {

    private final ProductSearchRepository productSearchRepository;

    @Cacheable(cacheNames = "productSearch", key = "@searchCacheKeyGenerator.generate(#condition, #pageable)")
    public Page<ProductListItemResponse> search(ProductSearchCondition condition, Pageable pageable) {

        // 캐시 Miss일 때만 실행됩니다.
        log.info("DB 상품 검색 실행 - condition={}, pageable={}", condition, pageable);

        return productSearchRepository.search(condition, pageable);
    }
}
