package com.example.fivespringusedmarket.wish.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductImageRepository;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import com.example.fivespringusedmarket.wish.dto.WishProductResponse;
import com.example.fivespringusedmarket.wish.dto.WishStatusResponse;
import com.example.fivespringusedmarket.wish.entity.Wish;
import com.example.fivespringusedmarket.wish.repository.WishRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wish-facade-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=12345678901234567890123456789012",
        "jwt.access-token-expiration=3600000"
})
class WishFacadeTest {

    @Autowired
    private WishFacade wishFacade;

    @Autowired
    private WishService wishService;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @MockitoBean
    private S3PresignedUrlService s3PresignedUrlService;

    private Member buyer;
    private Member seller;

    @BeforeEach
    void setUp() {
        wishRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();

        buyer = memberRepository.saveAndFlush(
                Member.create("buyer@test.com", "encoded-password", "구매자")
        );

        seller = memberRepository.saveAndFlush(
                Member.create("seller@test.com", "encoded-password", "판매자")
        );

        // WishFacadeTest는 관심상품 조회 정책만 검증하므로 실제 AWS SDK Presigner를 호출하지 않는다.
        when(s3PresignedUrlService.createPresignedUrl(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class)
                        .replace("products/", "https://image.test/"));
    }

    @Test
    @DisplayName("관심상품 등록 - 다른 회원의 상품을 관심상품으로 등록한다")
    void addWishSuccess() {
        // given
        Product product = saveProduct(seller, "아이폰 15", 800000, ProductCategory.DIGITAL);

        // when
        WishStatusResponse response = wishFacade.addWish(buyer.getId(), product.getId());

        // then
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.wished()).isTrue();

        assertThat(wishRepository.existsByMemberIdAndProductId(buyer.getId(), product.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("관심상품 등록 - 이미 등록한 상품이면 예외가 발생한다")
    void addWishDuplicatedThrowsException() {
        // given
        Product product = saveProduct(seller, "아이폰 15", 800000, ProductCategory.DIGITAL);

        wishFacade.addWish(buyer.getId(), product.getId());

        // when & then
        assertThatThrownBy(() -> wishFacade.addWish(buyer.getId(), product.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WISH_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("관심상품 등록 - 본인 상품은 관심상품으로 등록할 수 없다")
    void addWishOwnProductThrowsException() {
        // given
        Product myProduct = saveProduct(buyer, "내 상품", 10000, ProductCategory.ETC);

        // when & then
        assertThatThrownBy(() -> wishFacade.addWish(buyer.getId(), myProduct.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WISH_OWN_PRODUCT_NOT_ALLOWED);
    }

    @Test
    @DisplayName("관심상품 등록 - 삭제된 상품은 관심상품으로 등록할 수 없다")
    void addWishDeletedProductThrowsException() {
        // given
        Product deletedProduct = saveProduct(seller, "삭제된 상품", 10000, ProductCategory.ETC);
        deletedProduct.updateStatus(ProductStatus.DELETED);
        productRepository.saveAndFlush(deletedProduct);

        // when & then
        assertThatThrownBy(() -> wishFacade.addWish(buyer.getId(), deletedProduct.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("관심상품 등록 - 판매완료 상품도 관심상품으로 등록할 수 있다")
    void addWishSoldProductSuccess() {
        // given
        Product soldProduct = saveProduct(seller, "판매완료 상품", 10000, ProductCategory.ETC);
        soldProduct.updateStatus(ProductStatus.SOLD);
        productRepository.saveAndFlush(soldProduct);

        // when
        WishStatusResponse response = wishFacade.addWish(buyer.getId(), soldProduct.getId());

        // then
        assertThat(response.productId()).isEqualTo(soldProduct.getId());
        assertThat(response.wished()).isTrue();

        assertThat(wishRepository.existsByMemberIdAndProductId(buyer.getId(), soldProduct.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("관심상품 취소 - 등록된 관심상품을 취소한다")
    void removeWishSuccess() {
        // given
        Product product = saveProduct(seller, "아이폰 15", 800000, ProductCategory.DIGITAL);

        wishFacade.addWish(buyer.getId(), product.getId());

        // when
        WishStatusResponse response = wishFacade.removeWish(buyer.getId(), product.getId());

        // then
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.wished()).isFalse();

        assertThat(wishRepository.existsByMemberIdAndProductId(buyer.getId(), product.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("관심상품 취소 - 관심상품으로 등록하지 않은 상품이면 예외가 발생한다")
    void removeWishNotFoundThrowsException() {
        // given
        Product product = saveProduct(seller, "아이폰 15", 800000, ProductCategory.DIGITAL);

        // when & then
        assertThatThrownBy(() -> wishFacade.removeWish(buyer.getId(), product.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WISH_NOT_FOUND);
    }

    @Test
    @DisplayName("관심상품 목록 조회 - 최신 관심 등록순으로 조회한다")
    void getMyWishesReturnsLatestOrder() throws InterruptedException {
        // given
        Product firstProduct = saveProduct(seller, "첫 번째 상품", 10000, ProductCategory.ETC);
        Product secondProduct = saveProduct(seller, "두 번째 상품", 20000, ProductCategory.DIGITAL);

        wishFacade.addWish(buyer.getId(), firstProduct.getId());
        Thread.sleep(10);
        wishFacade.addWish(buyer.getId(), secondProduct.getId());

        // when
        List<WishProductResponse> response = wishFacade.getMyWishes(buyer.getId());

        // then
        assertThat(response)
                .extracting(WishProductResponse::productId)
                .containsExactly(secondProduct.getId(), firstProduct.getId());
    }

    @Test
    @DisplayName("관심상품 목록 조회 - 삭제된 상품은 제외하고 판매완료 상품은 포함한다")
    void getMyWishesExcludesDeletedAndIncludesSold() {
        // given
        Product onSaleProduct = saveProduct(seller, "판매중 상품", 10000, ProductCategory.ETC);

        Product soldProduct = saveProduct(seller, "판매완료 상품", 20000, ProductCategory.DIGITAL);
        soldProduct.updateStatus(ProductStatus.SOLD);
        productRepository.saveAndFlush(soldProduct);

        Product deletedProduct = saveProduct(seller, "삭제된 상품", 30000, ProductCategory.SPORTS);
        deletedProduct.updateStatus(ProductStatus.DELETED);
        productRepository.saveAndFlush(deletedProduct);

        wishRepository.save(Wish.create(buyer, onSaleProduct));
        wishRepository.save(Wish.create(buyer, soldProduct));
        wishRepository.save(Wish.create(buyer, deletedProduct));

        // when
        List<WishProductResponse> response = wishFacade.getMyWishes(buyer.getId());

        // then
        assertThat(response)
                .extracting(WishProductResponse::productId)
                .containsExactlyInAnyOrder(onSaleProduct.getId(), soldProduct.getId());

        assertThat(response)
                .extracting(WishProductResponse::productId)
                .doesNotContain(deletedProduct.getId());
    }

    @Test
    @DisplayName("관심상품 목록 조회 - 대표 이미지 URL을 포함한다")
    void getMyWishesContainsThumbnailUrl() {
        // given
        Product product = saveProduct(seller, "이미지 상품", 10000, ProductCategory.ETC);

        wishFacade.addWish(buyer.getId(), product.getId());

        // when
        List<WishProductResponse> response = wishFacade.getMyWishes(buyer.getId());

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).thumbnailUrl())
                .isEqualTo("https://image.test/" + product.getTitle() + ".png");
    }

    @Test
    @DisplayName("관심상품 여부 확인 - 관심상품으로 등록되어 있으면 true를 반환한다")
    void isWishedReturnsTrue() {
        // given
        Product product = saveProduct(seller, "아이폰 15", 800000, ProductCategory.DIGITAL);

        wishFacade.addWish(buyer.getId(), product.getId());

        // when
        boolean wished = wishService.isWished(buyer.getId(), product.getId());

        // then
        assertThat(wished).isTrue();
    }

    @Test
    @DisplayName("관심상품 여부 확인 - 관심상품으로 등록되어 있지 않으면 false를 반환한다")
    void isWishedReturnsFalse() {
        // given
        Product product = saveProduct(seller, "아이폰 15", 800000, ProductCategory.DIGITAL);

        // when
        boolean wished = wishService.isWished(buyer.getId(), product.getId());

        // then
        assertThat(wished).isFalse();
    }

    @Test
    @DisplayName("관심상품 여부 확인 - 비로그인 사용자는 false를 반환한다")
    void isWishedReturnsFalseWhenMemberIdIsNull() {
        // given
        Product product = saveProduct(seller, "아이폰 15", 800000, ProductCategory.DIGITAL);

        // when
        boolean wished = wishService.isWished(null, product.getId());

        // then
        assertThat(wished).isFalse();
    }

    @Test
    @DisplayName("관심상품 개수 조회 - 삭제된 상품은 개수에서 제외한다")
    void countMyWishesExcludesDeletedProduct() {
        // given
        Product onSaleProduct = saveProduct(seller, "판매중 상품", 10000, ProductCategory.ETC);

        Product deletedProduct = saveProduct(seller, "삭제된 상품", 20000, ProductCategory.DIGITAL);
        deletedProduct.updateStatus(ProductStatus.DELETED);
        productRepository.saveAndFlush(deletedProduct);

        wishRepository.save(Wish.create(buyer, onSaleProduct));
        wishRepository.save(Wish.create(buyer, deletedProduct));

        // when
        long count = wishService.countMyWishes(buyer.getId());

        // then
        assertThat(count).isEqualTo(1);
    }

    private Product saveProduct(
            Member seller,
            String title,
            int price,
            ProductCategory category
    ) {
        Product product = productRepository.saveAndFlush(
                Product.create(seller, title, "테스트 상품 설명", price, category)
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
