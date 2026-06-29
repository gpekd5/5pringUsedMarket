package com.example.fivespringusedmarket.search.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private final CachedProductSearchReader cachedProductSearchService;
    private static final String POPULAR_SEARCH_KEY  = "popular:keywords";
    private final S3PresignedUrlService s3PresignedUrlService;
    private static final int POPULAR_SEARCH_LIMIT = 10;

    /**
     * 캐시가 적용되지 않은 상품 검색 v1 기능입니다.
     *
     * <p>검색 조건을 파싱한 뒤 QueryDSL Repository에 전달하여 상품 목록을 조회합니다.
     * 로그인 사용자가 keyword로 검색한 경우에는 최근 검색어 DB 저장과 Redis 인기검색어 집계를 함께 수행합니다.</p>
     */
    @Transactional
    public ProductPageResponse searchProductsV1(Member member,String keyword, String category, String status, String sort, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        ProductSearchCondition condition = new ProductSearchCondition(
                normalizedKeyword,
                parseCategory(category),
                parseStatus(status),
                parseSort(sort)
        );

        saveSearchLog(member, normalizedKeyword);

        // 검색 Repository는 DB에 저장된 대표 이미지 key를 가져오고, Service에서 응답용 URL로 변환한다.
        Page<ProductListItemResponse> products = productSearchRepository.search(condition, pageable)
                .map(this::withPresignedThumbnailUrl);

        return ProductPageResponse.of(products);
    }

    /**
     * Caffeine 캐시가 적용된 상품 검색 v2 기능입니다.
     *
     * <p>검색 로그 저장과 인기검색어 집계는 매 요청마다 수행하고,
     * 상품 목록 조회 결과만 캐시를 적용합니다.</p>
     */
    @Transactional
    public ProductPageResponse searchProductsV2(Member member, String keyword, String category, String status, String sort, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        ProductSearchCondition condition = new ProductSearchCondition(
                normalizedKeyword,
                parseCategory(category),
                parseStatus(status),
                parseSort(sort)
        );

        // 검색 행위 기록은 캐시 여부와 상관없이 매번 저장합니다.
        saveSearchLog(member, normalizedKeyword);

        // 실제 상품 목록 조회만 캐시 적용 Service에 위임합니다.
        ProductPageResponse response =
                cachedProductSearchService.searchWithCaffeine(condition, pageable);

        return withPresignedThumbnailUrls(response);
    }

    /**
     * Redis 캐시가 적용된 상품 검색 v3 기능입니다.
     *
     * <p>검색 로그 저장과 인기검색어 집계는 매 요청마다 수행하고,
     * 상품 목록 조회 결과만 캐시를 적용합니다.</p>
     */
    @Transactional
    public ProductPageResponse searchProductsV3(Member member, String keyword, String category, String status, String sort, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        ProductSearchCondition condition = new ProductSearchCondition(
                normalizedKeyword,
                parseCategory(category),
                parseStatus(status),
                parseSort(sort)
        );

        // 검색 행위 기록은 캐시 여부와 상관없이 매번 저장합니다.
        saveSearchLog(member, normalizedKeyword);

        ProductPageResponse response =
                cachedProductSearchService.searchWithRedis(condition, pageable);

        return withPresignedThumbnailUrls(response);
    }

    /**
     * 로그인 사용자의 최근 검색어 목록을 조회합니다.
     *
     * <p>DB에는 모든 검색 기록을 저장하지만,
     * 응답에서는 같은 keyword가 여러 번 존재하더라도 최신 검색 기록 기준으로 하나만 노출합니다.</p>
     */
    public List<RecentSearchResponse> getRecentSearches(Long memberId) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        return searchLogRepository.findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(memberId, oneMonthAgo)
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

        searchLogRepository.deleteByMemberIdAndKeyword(
                memberId,
                searchLog.getKeyword()
        );
    }

    /**
     * Redis ZSet에 저장된 인기검색어 Top 10을 조회합니다.
     *
     * <p>인기검색어는 로그인 사용자의 keyword 검색만 기준으로 집계됩니다.
     * Redis ZSet의 score가 높은 순서대로 최대 10개를 반환합니다.</p>
     */
    public List<PopularSearchResponse> getPopularSearches() {
        Set<ZSetOperations.TypedTuple<String>> popularKeywords =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_SEARCH_KEY, 0, POPULAR_SEARCH_LIMIT-1);

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

        // 최근 검색어 조회/삭제를 위한 DB 검색 기록 저장
        searchLogRepository.save(SearchLog.create(member, keyword));
        // 인기검색어 Top 10 조회를 위한 Redis ZSet score 증가
        stringRedisTemplate.opsForZSet().incrementScore(POPULAR_SEARCH_KEY, keyword, 1);
    }

    /**
     * keyword를 Trim하여 변환합니다.
     *
     */
    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
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

    /**
     * 검색 목록 응답 안의 thumbnailUrl(imageKey)을 Presigned URL로 변환한다.
     */
    private ProductPageResponse withPresignedThumbnailUrls(ProductPageResponse response) {
        List<ProductListItemResponse> convertedContent = response.content()
                .stream()
                .map(this::withPresignedThumbnailUrl)
                .toList();

        return new ProductPageResponse(
                convertedContent,
                response.page(),
                response.size(),
                response.totalElements(),
                response.totalPages()
        );
    }

    /**
     * 검색 목록의 thumbnailUrl 자리에 들어 있던 imageKey를 Presigned URL로 바꾼다.
     */
    private ProductListItemResponse withPresignedThumbnailUrl(ProductListItemResponse product) {
        return product.withThumbnailUrl(s3PresignedUrlService.createPresignedUrl(product.thumbnailUrl()));
    }
}
