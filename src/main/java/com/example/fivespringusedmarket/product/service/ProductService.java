package com.example.fivespringusedmarket.product.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.dto.CreateProductRequest;
import com.example.fivespringusedmarket.product.dto.MemberProfileResponse;
import com.example.fivespringusedmarket.product.dto.ProductResponse;
import com.example.fivespringusedmarket.product.dto.UpdateProductRequest;
import com.example.fivespringusedmarket.product.dto.ProductListItemResponse;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductImageRepository;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 등록 및 조회 비즈니스 로직을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberRepository memberRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * 상품 기본 정보와 업로드가 끝난 이미지 key 목록을 저장한다.
     *
     * <p>Controller는 MultipartFile을 받지 않는다. 클라이언트가 먼저 이미지 업로드 API를 호출해
     * imageKey를 받은 뒤, 그 key 목록을 상품 등록 요청에 담아 보내는 흐름이다.</p>
     */
    @Transactional
    @CacheEvict(cacheNames = "productSearch", allEntries = true)
    public ProductResponse createProduct(Long memberId, CreateProductRequest request) {
        if (request.price() < 0) {
            throw new CustomException(ErrorCode.INVALID_PRICE);
        }

        ProductCategory category = parseCategory(request.category());
        Member seller = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Product product = productRepository.save(
                Product.create(seller, request.title(), request.description(), request.price(), category)
        );

        List<ProductImage> images = saveImages(product, request.imageKeys());

        // 응답에는 DB에 저장한 key가 아니라, 즉시 조회 가능한 Presigned URL을 담는다.
        return ProductResponse.of(product, createPresignedUrls(images));
    }

    /**
     * 상품 정보를 수정하고, imageKeys 필드가 전달된 경우 기존 이미지 목록을 새 key 목록으로 교체한다.
     */
    @Transactional
    @CacheEvict(cacheNames = "productSearch", allEntries = true)
    public ProductResponse updateProduct(Long memberId, Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (product.isSold()) {
            throw new CustomException(ErrorCode.CANNOT_MODIFY_SOLD_PRODUCT);
        }

        ProductCategory category = request.category() != null ? parseCategory(request.category()) : null;
        product.update(request.title(), request.price(), request.description(), category);

        List<ProductImage> images = replaceImages(product, request.imageKeys());

        // 수정 응답도 상세 조회와 동일하게 Presigned URL 형태로 내려준다.
        return ProductResponse.of(product, createPresignedUrls(images));
    }

    @Transactional
    @CacheEvict(cacheNames = "productSearch", allEntries = true)
    public void deleteProduct(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        product.updateStatus(ProductStatus.DELETED);
    }

    /**
     * 상품 상세 정보를 조회한다.
     *
     * <p>DB의 ProductImage에는 imageKey만 있으므로, 응답 직전에 Presigned URL 목록으로 변환한다.</p>
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);

        return ProductResponse.of(product, createPresignedUrls(images));
    }

    /**
     * 로그인한 회원의 판매 상품 목록을 조회한다.
     *
     * <p>목록에서는 전체 이미지가 필요하지 않으므로 대표 이미지(sortOrder=0)만 Presigned URL로 변환한다.</p>
     */
    @Transactional(readOnly = true)
    public ProductPageResponse getMyProducts(Long memberId, String status, Pageable pageable) {
        ProductStatus statusEnum = status != null ? parseStatus(status) : null;

        // DELETED 상태는 내 판매 상품 목록에서 조회할 수 없다.
        if (statusEnum == ProductStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Page<Product> productPage = productRepository.findMyProducts(memberId, statusEnum, ProductStatus.DELETED, pageable);

        // 대표 이미지(sortOrder=0)를 상품 ID 기준으로 한 번에 조회해 N+1을 방지한다.
        List<Long> productIds = productPage.map(Product::getId).toList();
        Map<Long, String> thumbnailUrlMap = productImageRepository
                .findByProductIdInAndSortOrder(productIds, 0)
                .stream()
                .collect(Collectors.toMap(
                        img -> img.getProduct().getId(),
                        img -> s3PresignedUrlService.createPresignedUrl(img.getImageKey())
                ));

        Page<ProductListItemResponse> responsePage = productPage.map(product ->
                ProductListItemResponse.of(product, thumbnailUrlMap.get(product.getId()))
        );

        return ProductPageResponse.of(responsePage);
    }

    /**
     * 공개 상품 목록을 검색 조건과 함께 조회한다.
     *
     * <p>목록 응답의 thumbnailUrl에는 대표 이미지 key를 변환한 Presigned URL이 들어간다.</p>
     */
    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(String category, String keyword, String status, Long sellerId, Pageable pageable) {
        ProductCategory categoryEnum = category != null ? parseCategory(category) : null;
        ProductStatus statusEnum = status != null ? parseStatus(status) : ProductStatus.ON_SALE;

        // DELETED 상태는 공개 목록에서 조회할 수 없다.
        if (statusEnum == ProductStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Page<Product> productPage = productRepository.searchProducts(categoryEnum, keyword, statusEnum, sellerId, ProductStatus.DELETED, pageable);

        // 대표 이미지(sortOrder=0)를 상품 ID 기준으로 한 번에 조회해 N+1을 방지한다.
        List<Long> productIds = productPage.map(Product::getId).toList();
        Map<Long, String> thumbnailUrlMap = productImageRepository
                .findByProductIdInAndSortOrder(productIds, 0)
                .stream()
                .collect(Collectors.toMap(
                        img -> img.getProduct().getId(),
                        img -> s3PresignedUrlService.createPresignedUrl(img.getImageKey())
                ));

        Page<ProductListItemResponse> responsePage = productPage.map(product ->
                ProductListItemResponse.of(product, thumbnailUrlMap.get(product.getId()))
        );

        return ProductPageResponse.of(responsePage);
    }

    @Transactional
    @CacheEvict(cacheNames = "productSearch", allEntries = true)
    public void updateProductStatus(Long memberId, Long productId, String status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        ProductStatus next;
        try {
            next = ProductStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_STATUS);
        }

        // DELETED, RESERVED→ON_SALE 전이는 이 API에서 허용하지 않는다.
        if (next == ProductStatus.DELETED || !product.canTransitionTo(next)) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        product.updateStatus(next);
    }

    @Transactional
    @CacheEvict(cacheNames = "productSearch", allEntries = true)
    public void cancelReservation(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (product.getStatus() != ProductStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        product.updateStatus(ProductStatus.ON_SALE);
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getMemberProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        long productCount = productRepository.countBySellerIdAndStatusNot(memberId, ProductStatus.DELETED);

        return MemberProfileResponse.of(member, productCount);
    }

    private ProductCategory parseCategory(String value) {
        try {
            return ProductCategory.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CATEGORY);
        }
    }

    private ProductStatus parseStatus(String value) {
        try {
            return ProductStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 수정 요청에서 imageKeys가 전달되었을 때 기존 이미지를 모두 삭제하고 새 목록을 저장한다.
     *
     * <p>{@code null}은 "이미지 변경 없음"으로 해석하고, 빈 배열은 "이미지 전체 삭제"로 해석한다.</p>
     */
    private List<ProductImage> replaceImages(Product product, List<String> imageKeys) {
        if (imageKeys == null) {
            // imageKeys 필드가 없으면 기존 이미지를 유지한다.
            return productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        }

        productImageRepository.deleteByProductId(product.getId());
        return saveImages(product, imageKeys);
    }

    /**
     * 상품 이미지 key 목록을 ProductImage 엔티티로 저장한다.
     */
    private List<ProductImage> saveImages(Product product, List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return new ArrayList<>();
        }

        // 요청 배열 순서 기준으로 0부터 sortOrder를 부여한다.
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageKeys.size(); i++) {
            images.add(ProductImage.create(product, imageKeys.get(i), i));
        }

        return productImageRepository.saveAll(images);
    }

    /**
     * DB에서 조회한 imageKey 목록을 클라이언트 응답용 Presigned URL 목록으로 변환한다.
     */
    private List<String> createPresignedUrls(List<ProductImage> images) {
        return images.stream()
                .map(ProductImage::getImageKey)
                .map(s3PresignedUrlService::createPresignedUrl)
                .toList();
    }
}
