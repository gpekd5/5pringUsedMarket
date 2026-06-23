package com.example.fivespringusedmarket.product.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.common.security.AuthMember;
import com.example.fivespringusedmarket.product.dto.CreateProductRequest;
import com.example.fivespringusedmarket.product.dto.CreateProductResponse;
import com.example.fivespringusedmarket.product.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	@PostMapping
	public ResponseEntity<ApiResponse<CreateProductResponse>> createProduct(
		@AuthenticationPrincipal AuthMember authMember,
		@Valid @RequestBody CreateProductRequest request
	) {
		CreateProductResponse response = productService.createProduct(authMember.memberId(), request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success("상품이 등록되었습니다.", response));
	}
}
