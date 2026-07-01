package com.example.fivespringusedmarket.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.dto.CreateProductRequest;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.dto.ProductResponse;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductImageRepository;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import com.example.fivespringusedmarket.wish.repository.WishRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @Mock
    private WishRepository wishRepository;

    private ProductService productService;
    private Member seller;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                productImageRepository,
                memberRepository,
                s3PresignedUrlService,
                wishRepository
        );

        seller = Member.create("seller@test.com", "encoded-password", "판매자");
        ReflectionTestUtils.setField(seller, "id", 1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createProductSavesImageKeys() {
        // given
        Product product = createProduct(10L);
        CreateProductRequest request = new CreateProductRequest(
                "맥북",
                1000000,
                "상태 좋습니다",
                "DIGITAL",
                List.of(
                        "products/11111111-1111-1111-1111-111111111111.png",
                        "products/22222222-2222-2222-2222-222222222222.jpg"
                )
        );

        when(memberRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productImageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3PresignedUrlService.createPresignedUrl("products/11111111-1111-1111-1111-111111111111.png"))
                .thenReturn("https://presigned.test/a");
        when(s3PresignedUrlService.createPresignedUrl("products/22222222-2222-2222-2222-222222222222.jpg"))
                .thenReturn("https://presigned.test/b");

        // when
        ProductResponse response = productService.createProduct(1L, request);

        // then
        ArgumentCaptor<List<ProductImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(productImageRepository).saveAll(imagesCaptor.capture());

        List<ProductImage> images = imagesCaptor.getValue();
        assertThat(images).extracting(ProductImage::getImageKey)
                .containsExactly(
                        "products/11111111-1111-1111-1111-111111111111.png",
                        "products/22222222-2222-2222-2222-222222222222.jpg"
                );
        assertThat(response.imageUrls())
                .containsExactly("https://presigned.test/a", "https://presigned.test/b");
    }

    @Test
    void createProductRejectsInvalidImageKey() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "맥북",
                1000000,
                "상태 좋습니다",
                "DIGITAL",
                List.of("products/not-uuid.png")
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_KEY);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProductRejectsImageKeyOutsideProductsPrefix() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "맥북",
                1000000,
                "상태 좋습니다",
                "DIGITAL",
                List.of("profiles/11111111-1111-1111-1111-111111111111.png")
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_KEY);
    }

    @Test
    void createProductRejectsUrlImageKey() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "맥북",
                1000000,
                "상태 좋습니다",
                "DIGITAL",
                List.of("https://bucket.s3.ap-northeast-2.amazonaws.com/products/11111111-1111-1111-1111-111111111111.png")
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_KEY);
    }

    @Test
    void createProductRejectsWebpImageKey() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "맥북",
                1000000,
                "상태 좋습니다",
                "DIGITAL",
                List.of("products/11111111-1111-1111-1111-111111111111.webp")
        );

        // when & then
        assertThatThrownBy(() -> productService.createProduct(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_KEY);
    }

    @Test
    void createProductRejectsImageKeyWhenS3ObjectDoesNotExist() {
        // given
        String imageKey = "products/11111111-1111-1111-1111-111111111111.png";
        CreateProductRequest request = new CreateProductRequest(
                "맥북",
                1000000,
                "상태 좋습니다",
                "DIGITAL",
                List.of(imageKey)
        );

        doThrow(new CustomException(ErrorCode.INVALID_IMAGE_KEY))
                .when(s3PresignedUrlService)
                .validateUploadedImageExists(imageKey);

        // when & then
        assertThatThrownBy(() -> productService.createProduct(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_KEY);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProductReturnsEmptyImageUrlsWhenProductHasNoImage() {
        // given
        Product product = createProduct(10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(10L)).thenReturn(List.of());

        // when
        ProductResponse response = productService.getProduct(10L, null);

        // then
        assertThat(response.imageUrls()).isEmpty();
    }

    @Test
    void getProductReturnsPresignedImageUrls() {
        // given
        Product product = createProduct(10L);
        ProductImage image = ProductImage.create(product, "products/33333333-3333-3333-3333-333333333333.png", 0);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(10L)).thenReturn(List.of(image));
        when(s3PresignedUrlService.createPresignedUrl("products/33333333-3333-3333-3333-333333333333.png"))
                .thenReturn("https://presigned.test/detail");

        // when
        ProductResponse response = productService.getProduct(10L, null);

        // then
        assertThat(response.imageUrls()).containsExactly("https://presigned.test/detail");
    }

    @Test
    void getProductsReturnsPresignedThumbnailUrl() {
        // given
        Product product = createProduct(10L);
        ProductImage thumbnail = ProductImage.create(product, "products/44444444-4444-4444-4444-444444444444.png", 0);
        PageRequest pageable = PageRequest.of(0, 10);

        when(productRepository.searchProducts(null, null, ProductStatus.ON_SALE, null, ProductStatus.DELETED, pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(productImageRepository.findByProductIdInAndSortOrder(List.of(10L), 0))
                .thenReturn(List.of(thumbnail));
        when(s3PresignedUrlService.createPresignedUrl("products/44444444-4444-4444-4444-444444444444.png"))
                .thenReturn("https://presigned.test/thumb");

        // when
        ProductPageResponse response = productService.getProducts(null, null, null, null, pageable);

        // then
        assertThat(response.content().get(0).thumbnailUrl()).isEqualTo("https://presigned.test/thumb");
    }

    private Product createProduct(Long productId) {
        Product product = Product.create(seller, "맥북", "상태 좋습니다", 1000000, ProductCategory.DIGITAL);
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }
}
