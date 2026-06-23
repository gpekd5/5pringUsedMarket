package com.example.fivespringusedmarket.product.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.dto.CreateProductRequest;
import com.example.fivespringusedmarket.product.dto.CreateProductResponse;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductImage;
import com.example.fivespringusedmarket.product.repository.ProductImageRepository;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CreateProductResponse createProduct(Long memberId, CreateProductRequest request) {
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

        return CreateProductResponse.of(product, images);
    }

    private ProductCategory parseCategory(String value) {
        try {
            return ProductCategory.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CATEGORY);
        }
    }

    private List<ProductImage> saveImages(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ProductImage.create(product, imageUrls.get(i), i + 1));
        }

        return productImageRepository.saveAll(images);
    }
}
