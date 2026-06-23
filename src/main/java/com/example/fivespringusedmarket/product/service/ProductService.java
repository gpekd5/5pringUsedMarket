package com.example.fivespringusedmarket.product.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.dto.CreateProductRequest;
import com.example.fivespringusedmarket.product.dto.DeleteProductResponse;
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

    @Transactional
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

        List<ProductImage> images = saveImages(product, request.images());

        return ProductResponse.of(product, images);
    }

    @Transactional
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

        List<ProductImage> images = replaceImages(product, request.images());

        return ProductResponse.of(product, images);
    }

    @Transactional
    public void deleteProduct(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        product.updateStatus(ProductStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);

        return ProductResponse.of(product, images);
    }

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(String category, String keyword, String status, Long sellerId, Pageable pageable) {
        ProductCategory categoryEnum = category != null ? parseCategory(category) : null;
        ProductStatus statusEnum = status != null ? parseStatus(status) : ProductStatus.ON_SALE;

        // DELETED 상태는 공개 목록에서 조회할 수 없다.
        if (statusEnum == ProductStatus.DELETED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Page<Product> productPage = productRepository.searchProducts(categoryEnum, keyword, statusEnum, sellerId, ProductStatus.DELETED, pageable);

        // 대표 이미지(sortOrder=1)를 상품 ID 기준으로 한 번에 조회해 N+1을 방지한다.
        List<Long> productIds = productPage.map(Product::getId).toList();
        Map<Long, String> imageUrlMap = productImageRepository
                .findByProductIdInAndSortOrder(productIds, 0)
                .stream()
                .collect(Collectors.toMap(img -> img.getProduct().getId(), ProductImage::getImageUrl));

        Page<ProductListItemResponse> responsePage = productPage.map(product ->
                ProductListItemResponse.of(product, imageUrlMap.get(product.getId()))
        );

        return ProductPageResponse.of(responsePage);
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

    private List<ProductImage> replaceImages(Product product, List<String> imageUrls) {
        if (imageUrls == null) {
            // images 필드가 없으면 기존 이미지를 유지한다.
            return productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        }

        productImageRepository.deleteByProductId(product.getId());
        return saveImages(product, imageUrls);
    }

    private List<ProductImage> saveImages(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ArrayList<>();
        }

        // 요청 배열 순서 기준으로 1부터 sortOrder를 부여한다.
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ProductImage.create(product, imageUrls.get(i), i + 1));
        }

        return productImageRepository.saveAll(images);
    }
}
