package com.example.fivespringusedmarket.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
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
import com.example.fivespringusedmarket.search.repository.SearchLogRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search-service-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
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
    }

    @Test
    void keywordSearchFindsProductsByTitleOrDescription() {
        // given
        Product titleMatched = saveProduct(
                "아이폰 15 팝니다",
                "상태 좋습니다",
                800000,
                ProductCategory.DIGITAL
        );

        Product descriptionMatched = saveProduct(
                "갤럭시 팝니다",
                "아이폰과 교환 가능합니다",
                600000,
                ProductCategory.DIGITAL
        );

        Product notMatched = saveProduct(
                "자전거 팝니다",
                "거의 새 상품입니다",
                100000,
                ProductCategory.SPORTS
        );

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
        List<Long> productIds = response.content().stream()
                .map(ProductListItemResponse::productId)
                .toList();

        assertThat(productIds)
                .containsExactlyInAnyOrder(titleMatched.getId(), descriptionMatched.getId());

        assertThat(productIds)
                .doesNotContain(notMatched.getId());
    }

    @Test
    void categoryAndStatusSearchFindsMatchingProductsOnly() {
        // given
        Product reservedSports = saveProduct(
                "예약중 자전거",
                "스포츠 상품입니다",
                100000,
                ProductCategory.SPORTS
        );
        reservedSports.updateStatus(ProductStatus.RESERVED);
        productRepository.saveAndFlush(reservedSports);

        Product onSaleSports = saveProduct(
                "판매중 자전거",
                "스포츠 상품입니다",
                120000,
                ProductCategory.SPORTS
        );

        Product reservedDigital = saveProduct(
                "예약중 노트북",
                "디지털 상품입니다",
                700000,
                ProductCategory.DIGITAL
        );
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
        List<Long> productIds = response.content().stream()
                .map(ProductListItemResponse::productId)
                .toList();

        assertThat(productIds).containsExactly(reservedSports.getId());
        assertThat(productIds).doesNotContain(onSaleSports.getId(), reservedDigital.getId());
    }

    @Test
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
        List<Integer> prices = response.content().stream()
                .map(ProductListItemResponse::price)
                .toList();

        assertThat(prices).containsExactly(10000, 20000, 30000);
    }

    @Test
    void searchResultContainsThumbnailUrl() {
        // given
        Product product = saveProduct(
                "이미지 있는 상품",
                "대표 이미지 테스트",
                10000,
                ProductCategory.ETC
        );

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
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productId()).isEqualTo(product.getId());
        assertThat(response.content().get(0).thumbnailUrl())
                .isEqualTo("https://image.test/" + product.getTitle() + ".png");
    }

    @Test
    void loginUserKeywordSearchSavesSearchLog() {
        // given
        saveProduct(
                "아이폰 15 팝니다",
                "상태 좋습니다",
                800000,
                ProductCategory.DIGITAL
        );

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
        assertThat(searchLogRepository.findAll()).hasSize(1);
        assertThat(searchLogRepository.findAll().get(0).getMember().getId()).isEqualTo(seller.getId());
        assertThat(searchLogRepository.findAll().get(0).getKeyword()).isEqualTo("아이폰");
    }

    @Test
    void guestKeywordSearchDoesNotSaveSearchLog() {
        // given
        saveProduct(
                "아이폰 15 팝니다",
                "상태 좋습니다",
                800000,
                ProductCategory.DIGITAL
        );

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
        assertThat(searchLogRepository.findAll()).isEmpty();
    }

    @Test
    void blankKeywordDoesNotSaveSearchLog() {
        // given
        saveProduct(
                "아이폰 15 팝니다",
                "상태 좋습니다",
                800000,
                ProductCategory.DIGITAL
        );

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
        assertThat(searchLogRepository.findAll()).isEmpty();
    }

    @Test
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
                        "https://image.test/" + title + ".png",
                        0
                )
        );

        return product;
    }
}