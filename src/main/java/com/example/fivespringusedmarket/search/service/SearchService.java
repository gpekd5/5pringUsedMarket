package com.example.fivespringusedmarket.search.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.search.dto.PopularSearchResponse;
import com.example.fivespringusedmarket.search.dto.ProductSearchCondition;
import com.example.fivespringusedmarket.search.dto.ProductSearchSortType;
import com.example.fivespringusedmarket.search.dto.RecentSearchResponse;
import com.example.fivespringusedmarket.search.entity.SearchLog;
import com.example.fivespringusedmarket.search.repository.ProductSearchRepository;
import com.example.fivespringusedmarket.search.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 상품 검색 비즈니스 로직을 처리하는 Service입니다.
 *
 * <p>검색 조건과 페이징 정보를 Repository에 전달하고,
 * 조회 결과를 상품 목록 페이지 응답 형태로 변환합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final ProductSearchRepository productSearchRepository;
    private final SearchLogRepository searchLogRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String RANKING_POST_KEY = "popular:keywords";
    private static final int POPULAR_SEARCH_LIMIT = 10;

    /**
     * 캐시가 적용되지 않은 상품 검색 v1 기능입니다.
     *
     * <p>검색 조건을 파싱한 뒤 QueryDSL Repository에 전달하여 상품 목록을 조회합니다.
     * 로그인 사용자가 keyword로 검색한 경우에는 최근 검색어 DB 저장과 Redis 인기검색어 집계를 함께 수행합니다.</p>
     */
    @Transactional
    public ProductPageResponse searchProductsV1(Member member,String keyword, String category, String status, String sort, Pageable pageable)
    {
        ProductSearchCondition condition = new ProductSearchCondition(
                keyword,
                parseCategory(category),
                parseStatus(status),
                parseSort(sort)
        );

        saveSearchLog(member, keyword);

        Page<ProductListItemResponse> products = productSearchRepository.search(condition, pageable);

        return ProductPageResponse.of(products);
    }

    /**
     * 로그인 사용자의 최근 검색어 목록을 조회합니다.
     *
     * <p>DB에는 모든 검색 기록을 저장하지만,
     * 응답에서는 같은 keyword가 여러 번 존재하더라도 최신 검색 기록 기준으로 하나만 노출합니다.</p>
     */
    public List<RecentSearchResponse> getRecentSearches(Long memberId) {
        return searchLogRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .filter(distinctByKeyword())
                .limit(10)
                .map(RecentSearchResponse::from)
                .toList();
    }

    /**
     * 로그인 사용자의 최근 검색어를 삭제합니다.
     *
     * <p>검색 기록은 본인 것만 삭제할 수 있으므로,
     * 인증 사용자 ID와 검색 기록의 memberId를 비교해 권한을 검증합니다.</p>
     */
    @Transactional
    public void deleteRecentSearch(Long memberId, Long searchLogId) {
        SearchLog searchLog = searchLogRepository.findById(searchLogId).orElseThrow(
                () -> new CustomException(ErrorCode.SEARCH_LOG_NOT_FOUND)
        );

        if (!searchLog.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_SEARCH_LOG);
        }

        searchLogRepository.delete(searchLog);
    }

    /**
     * Redis ZSet에 저장된 인기검색어 Top 10을 조회합니다.
     *
     * <p>인기검색어는 로그인 사용자의 keyword 검색만 기준으로 집계됩니다.
     * Redis ZSet의 score가 높은 순서대로 최대 10개를 반환합니다.</p>
     */
    public List<PopularSearchResponse> getPopularSearches() {
        Set<ZSetOperations.TypedTuple<String>> popularKeywords =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(RANKING_POST_KEY, 0, POPULAR_SEARCH_LIMIT-1);

        if (popularKeywords == null || popularKeywords.isEmpty()) {
            return List.of();
        }

        return popularKeywords.stream()
                .map(tuple -> new PopularSearchResponse(
                        String.valueOf(tuple.getValue()),
                        toLongScore(tuple.getScore())
                ))
                .toList();

    }

    /**
     * 최신순 검색 기록에서 keyword 중복을 제거하기 위한 Predicate입니다.
     *
     * <p>검색 기록을 최신순으로 조회한 뒤 처음 등장한 keyword만 남기므로,
     * 같은 keyword 중 가장 최근 검색 기록이 응답에 포함됩니다.</p>
     */
    private Predicate<SearchLog> distinctByKeyword() {
        Set<String> keywords = new HashSet<>();

        return searchLog -> keywords.add(searchLog.getKeyword());
    }

    /**
     * Redis ZSet score를 응답용 Long 타입으로 변환합니다.
     */
    private Long toLongScore(Double score) {
        return score == null ? 0L : score.longValue();
    }

    /**
     * 로그인 사용자의 keyword 검색어를 저장하고 인기검색어 점수를 증가시킵니다.
     *
     * <p>비로그인 사용자이거나 keyword가 비어 있는 경우에는 아무 작업도 하지 않습니다.
     * DB에는 최근 검색어 조회/삭제를 위해 검색 기록을 저장하고,
     * Redis ZSet에는 인기검색어 조회를 위해 score를 증가시킵니다.</p>
     */
    private void saveSearchLog(Member member, String keyword) {
        if (member == null || !StringUtils.hasText(keyword)) {
            return;
        }

        String trimmedKeyword = keyword.trim();

        // 최근 검색어 조회/삭제를 위한 DB 검색 기록 저장
        searchLogRepository.save(SearchLog.create(member, trimmedKeyword)); // 앞뒤 공백 제거하기 위해 트림 사용
        // 인기검색어 Top 10 조회를 위한 Redis ZSet score 증가
        stringRedisTemplate.opsForZSet().incrementScore(RANKING_POST_KEY, trimmedKeyword, 1);
    }

    /**
     * 문자열 category 요청 값을 ProductCategory enum으로 변환합니다.
     */
    private ProductCategory parseCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }

        try{
            return ProductCategory.valueOf(category.toUpperCase());
        }catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CATEGORY);
        }
    }

    private ProductStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return ProductStatus.ON_SALE; // 일반 사용자가 상품 검색하면 보통 판매중 상품만 기본 노출
        }

        try{
            return ProductStatus.valueOf(status.toUpperCase());
        }catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_STATUS);
        }
    }

    private ProductSearchSortType parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return ProductSearchSortType.LATEST;
        }

        try{
            return ProductSearchSortType.valueOf(sort.toUpperCase());
        }catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_SORT_TYPE);
        }
    }
}
