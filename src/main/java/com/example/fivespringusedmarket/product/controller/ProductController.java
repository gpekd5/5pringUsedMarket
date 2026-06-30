package com.example.fivespringusedmarket.product.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.product.dto.CreateProductRequest;
import com.example.fivespringusedmarket.product.dto.ProductResponse;
import com.example.fivespringusedmarket.product.dto.UpdateProductRequest;
import com.example.fivespringusedmarket.product.dto.UpdateProductStatusRequest;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 관련 HTTP 요청을 처리하는 Controller다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(authMember.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("상품이 등록되었습니다.", response));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(authMember.memberId(), productId, request);
        return ResponseEntity.ok(ApiResponse.success("상품이 수정되었습니다.", response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId
    ) {
        productService.deleteProduct(authMember.memberId(), productId);
        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다.", null));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long memberId = authMember == null ? null : authMember.memberId();
        ProductResponse response = productService.getProduct(productId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<Void>> updateProductStatus(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        productService.updateProductStatus(authMember.memberId(), productId, request.status());
        return ResponseEntity.ok(ApiResponse.success("상품 상태가 변경되었습니다.", null));
    }

    @PatchMapping("/{productId}/status/cancel-reservation")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId
    ) {
        productService.cancelReservation(authMember.memberId(), productId);
        return ResponseEntity.ok(ApiResponse.success("예약이 취소되었습니다.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProductPageResponse>> getMyProducts(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ProductPageResponse response = productService.getMyProducts(authMember.memberId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sellerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ProductPageResponse response = productService.getProducts(category, keyword, status, sellerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
