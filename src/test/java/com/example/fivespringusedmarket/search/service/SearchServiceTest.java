package com.example.fivespringusedmarket.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductImageRepository;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import com.example.fivespringusedmarket.search.dto.RecentSearchResponse;
import com.example.fivespringusedmarket.search.entity.SearchLog;
import com.example.fivespringusedmarket.search.repository.SearchLogRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search-service-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=caffeine",
        "jwt.secret=12345678901234567890123456789012",
        "jwt.access-token-expiration=3600000"
})
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ZSetOperations<String, String> zSetOperations;

    @MockitoBean
    private S3PresignedUrlService s3PresignedUrlService;

    private Member seller;

    @BeforeEach
    void setUp() {
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        searchLogRepository.deleteAll();
        memberRepository.deleteAll();

        seller = memberRepository.saveAndFlush(
                Member.create("seller@test.com", "encoded-password", "판매자")
        );

        // 테스트 간 캐시 데이터가 섞이지 않도록 productSearch 캐시를 비운다.
        Cache productSearchCache = cacheManager.getCache("productSearch");
        if (productSearchCache != null) {
            productSearchCache.clear();
        }

        // 테스트에서는 실제 Redis에 연결하지 않고, Redis ZSet 동작을 Mock 처리한다.
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.incrementScore(anyString(), anyString(), eq(1.0))).thenReturn(1.0);

        // 테스트에서는 실제 S3 Presigner를 호출하지 않고, imageKey 변환 결과만 검증한다.
        when(s3PresignedUrlService.createPresignedUrl(anyString()))
                .thenAnswer(invocation -> "https://presigned.test/" + invocation.getArgument(0));
    }

    @Test
    @DisplayName("V1 검색 - keyword가 제목 또는 설명에 포함된 상품을 조회한다")
    void keywordSearchFindsProductsByTitleOrDescription() {
        // given
        Product titleMatched = saveProduct("아이폰 15 팝니다", "상태 좋습니다", 800000, ProductCategory.DIGITAL);
        Product descriptionMatched = saveProduct("갤럭시 팝니다", "아이폰과 교환 가능합니다", 600000, ProductCategory.DIGITAL);
        Product notMatched = saveProduct("자전거 팝니다", "거의 새 상품입니다", 100000, ProductCategory.SPORTS);

        // when
        ProductPageResponse response = searchService.searchProductsV1(
                seller,
                "아이폰",
                null,
                null,
                "LATEST",
                PageRequest.of(0, 10)
        );

        // then
        // 제목 또는 설명에 keyword가 포함된 상품만 응답에 포함된다.
        List<Long> productIds = response.content().stream()
                .map(ProductListItemResponse::productId)
                .toList();

        assertThat(productIds)
                .containsExactlyInAnyOrder(titleMatched.getId(), descriptionMatched.getId());

        assertThat(productIds).doesNotContain(notMatched.getId());
    }

    @Test
    @DisplayName("V1 검색 - category와 status 조건에 맞는 상품만 조회한다")
    void categoryAndStatusSearchFindsMatchingProductsOnly() {
        // given
        Product reservedSports = saveProduct("예약중 자전거", "스포츠 상품입니다", 100000, ProductCategory.SPORTS);
        reservedSports.updateStatus(ProductStatus.RESERVED);
        productRepository.saveAndFlush(reservedSports);

        Product onSaleSports = saveProduct("판매중 자전거", "스포츠 상품입니다", 120000, ProductCategory.SPORTS);

        Product reservedDigital = saveProduct("예약중 노트북", "디지털 상품입니다", 700000, ProductCategory.DIGITAL);
        reservedDigital.updateStatus(ProductStatus.RESERVED);
        productRepository.saveAndFlush(reservedDigital);

        // when
        ProductPageResponse response = searchService.searchProductsV1(
                seller,
                null,
                "SPORTS",
                "RESERVED",
                "LATEST",
                PageRequest.of(0, 10)
        );

        // then
        // 카테고리와 상태가 모두 일치하는 상품만 조회된다.
        List<Long> productIds = response.content().stream()
                .map(ProductListItemResponse::productId)
                .toList();

        assertThat(productIds).containsExactly(reservedSports.getId());
        assertThat(productIds).doesNotContain(onSaleSports.getId(), reservedDigital.getId());
    }

    @Test
    @DisplayName("V1 검색 - PRICE_ASC 정렬 시 가격이 낮은 순서대로 조회한다")
    void priceAscSortReturnsProductsByLowestPriceFirst() {
        // given
        saveProduct("고가 상품", "가격 정렬 테스트", 30000, ProductCategory.ETC);
        saveProduct("저가 상품", "가격 정렬 테스트", 10000, ProductCategory.ETC);
        saveProduct("중간 상품", "가격 정렬 테스트", 20000, ProductCategory.ETC);

        // when
        ProductPageResponse response = searchService.searchProductsV1(
                seller,
                null,
                null,
                null,
                "PRICE_ASC",
                PageRequest.of(0, 10)
        );

        // then
        // 상품 가격이 낮은 순서대로 정렬된다.
        List<Integer> prices = response.content().stream()
                .map(ProductListItemResponse::price)
                .toList();

        assertThat(prices).containsExactly(10000, 20000, 30000);
    }

    @Test
    @DisplayName("V1 검색 - 검색 결과에 대표 이미지 URL이 포함된다")
    void searchResultContainsThumbnailUrl() {
        // given
        Product product = saveProduct("이미지 있는 상품", "대표 이미지 테스트", 10000, ProductCategory.ETC);

        // when
        ProductPageResponse response = searchService.searchProductsV1(
                seller,
                "이미지",
                null,
                null,
                "LATEST",
                PageRequest.of(0, 10)
        );

        // then
        // 대표 이미지 sortOrder가 0인 imageKey가 Presigned URL로 변환되어 응답된다.
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productId()).isEqualTo(product.getId());
        assertThat(response.content().get(0).thumbnailUrl())
                .isEqualTo("https://presigned.test/products/" + product.getTitle() + ".png");
    }

    @Test
    @DisplayName("V1 검색 - 로그인 사용자가 keyword 검색하면 검색 로그를 저장한다")
    void loginUserKeywordSearchSavesSearchLog() {
        // given
        saveProduct("아이폰 15 팝니다", "상태 좋습니다", 800000, ProductCategory.DIGITAL);

        // when
        searchService.searchProductsV1(
                seller,
                " 아이폰 ",
                null,
                null,
                "LATEST",
                PageRequest.of(0, 10)
        );

        // then
        // keyword는 trim 처리되어 검색 로그에 저장된다.
        assertThat(searchLogRepository.findAll()).hasSize(1);
        assertThat(searchLogRepository.findAll().get(0).getMember().getId()).isEqualTo(seller.getId());
        assertThat(searchLogRepository.findAll().get(0).getKeyword()).isEqualTo("아이폰");
    }

    @Test
    @DisplayName("V1 검색 - 비로그인 사용자의 keyword 검색은 검색 로그를 저장하지 않는다")
    void guestKeywordSearchDoesNotSaveSearchLog() {
        // given
        saveProduct("아이폰 15 팝니다", "상태 좋습니다", 800000, ProductCategory.DIGITAL);

        // when
        searchService.searchProductsV1(
                null,
                "아이폰",
                null,
                null,
                "LATEST",
                PageRequest.of(0, 10)
        );

        // then
        // member가 없으면 검색 로그를 저장하지 않는다.
        assertThat(searchLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("V1 검색 - 공백 keyword는 검색 로그를 저장하지 않는다")
    void blankKeywordDoesNotSaveSearchLog() {
        // given
        saveProduct("아이폰 15 팝니다", "상태 좋습니다", 800000, ProductCategory.DIGITAL);

        // when
        searchService.searchProductsV1(
                seller,
                "   ",
                null,
                null,
                "LATEST",
                PageRequest.of(0, 10)
        );

        // then
        // keyword가 공백이면 검색어로 보지 않는다.
        assertThat(searchLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("V1 검색 - 잘못된 category 요청이면 CustomException을 던진다")
    void invalidCategoryThrowsCustomException() {
        // when & then
        assertThatThrownBy(() -> searchService.searchProductsV1(
                seller,
                null,
                "PHONE",
                null,
                "LATEST",
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CATEGORY);
    }

    @Test
    @DisplayName("V1 검색 - 잘못된 sort 요청이면 CustomException을 던진다")
    void invalidSortThrowsCustomException() {
        // when & then
        assertThatThrownBy(() -> searchService.searchProductsV1(
                seller,
                null,
                null,
                null,
                "LOW_PRICE",
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SEARCH_SORT_TYPE);
    }

    @Test
    @DisplayName("V2 검색 - 캐시 Hit가 발생해도 검색 로그는 매번 저장된다")
    void searchProductsV2AlwaysSavesSearchLogEvenWhenCacheHit() {
        // given
        saveProduct("아이폰 15 팝니다", "상태 좋습니다", 800000, ProductCategory.DIGITAL);

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        searchService.searchProductsV2(seller, "아이폰", null, null, "LATEST", pageable);
        searchService.searchProductsV2(seller, "아이폰", null, null, "LATEST", pageable);

        // then
        // 상품 목록 조회는 캐시되어도, 검색 행위 기록은 매 요청마다 저장된다.
        assertThat(searchLogRepository.findAll()).hasSize(2);

        assertThat(searchLogRepository.findAll())
                .extracting(SearchLog::getKeyword)
                .containsExactly("아이폰", "아이폰");
    }

    @Test
    @DisplayName("최근 검색어 - 같은 keyword는 최신 검색 기록 기준으로 하나만 반환한다")
    void recentSearchesReturnsDistinctKeywordsByLatestSearchOrder() throws InterruptedException {
        // given
        searchLogRepository.saveAndFlush(SearchLog.create(seller, "맥북"));
        Thread.sleep(10);

        searchLogRepository.saveAndFlush(SearchLog.create(seller, "아이폰"));
        Thread.sleep(10);

        searchLogRepository.saveAndFlush(SearchLog.create(seller, "키보드"));
        Thread.sleep(10);

        searchLogRepository.saveAndFlush(SearchLog.create(seller, "맥북"));
        Thread.sleep(10);

        searchLogRepository.saveAndFlush(SearchLog.create(seller, "아이폰"));

        // when
        List<RecentSearchResponse> response = searchService.getRecentSearches(seller.getId());

        // then
        // 최신순 조회 후 keyword 중복을 제거한다.
        assertThat(response)
                .extracting(RecentSearchResponse::keyword)
                .containsExactly("아이폰", "맥북", "키보드");
    }

    @Test
    @DisplayName("최근 검색어 - 최대 10개의 keyword만 반환한다")
    void recentSearchesReturnsOnlyTop10DistinctKeywords() throws InterruptedException {
        // given
        for (int i = 1; i <= 12; i++) {
            searchLogRepository.saveAndFlush(SearchLog.create(seller, "검색어" + i));
            Thread.sleep(10);
        }

        // when
        List<RecentSearchResponse> response = searchService.getRecentSearches(seller.getId());

        // then
        // 최신순 기준으로 10개까지만 반환한다.
        assertThat(response).hasSize(10);

        assertThat(response)
                .extracting(RecentSearchResponse::keyword)
                .containsExactly(
                        "검색어12",
                        "검색어11",
                        "검색어10",
                        "검색어9",
                        "검색어8",
                        "검색어7",
                        "검색어6",
                        "검색어5",
                        "검색어4",
                        "검색어3"
                );
    }

    @Test
    @DisplayName("최근 검색어 - 본인의 검색 기록만 반환한다")
    void recentSearchesReturnsOnlyOwnSearchLogs() {
        // given
        Member otherMember = memberRepository.saveAndFlush(
                Member.create("other@test.com", "encoded-password", "다른회원")
        );

        searchLogRepository.saveAndFlush(SearchLog.create(otherMember, "다른회원검색어"));
        searchLogRepository.saveAndFlush(SearchLog.create(seller, "내검색어"));

        // when
        List<RecentSearchResponse> response = searchService.getRecentSearches(seller.getId());

        // then
        // 다른 회원의 검색 기록은 포함하지 않는다.
        assertThat(response)
                .extracting(RecentSearchResponse::keyword)
                .containsExactly("내검색어");
    }

    private Product saveProduct(
            String title,
            String description,
            int price,
            ProductCategory category
    ) {
        Product product = productRepository.saveAndFlush(
                Product.create(seller, title, description, price, category)
        );

        productImageRepository.saveAndFlush(
                ProductImage.create(
                        product,
                        "products/" + title + ".png",
                        0
                )
        );

        return product;
    }
}
