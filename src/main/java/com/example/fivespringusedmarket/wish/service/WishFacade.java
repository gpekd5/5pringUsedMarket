package com.example.fivespringusedmarket.wish.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import com.example.fivespringusedmarket.search.repository.ProductSearchRepository;
import com.example.fivespringusedmarket.wish.dto.WishProductResponse;
import com.example.fivespringusedmarket.wish.dto.WishStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관심상품 기능의 요청 흐름을 조립하는 Facade입니다.
 *
 * <p>
 * 다른 도메인(Member, Product)의 조회 및 검증을 담당하고,
 * Wish 도메인의 비즈니스 로직은 WishService에 위임합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishFacade {

    private final WishService wishService;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Transactional
    public WishStatusResponse addWish(Long memberId, Long productId) {

        Member member = findMember(memberId);
        Product product = findProduct(productId);

        validateProductCanBeWished(memberId, product);

        wishService.addWish(member, product);

        return WishStatusResponse.of(productId, true);
    }

    @Transactional
    public WishStatusResponse removeWish(Long memberId, Long productId) {
        findProduct(productId);
        wishService.removeWish(memberId, productId);

        return WishStatusResponse.of(productId, false);
    }

    public List<WishProductResponse> getMyWishes(Long memberId) {
        findMember(memberId);
        return wishService.getMyWishes(memberId);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND)
        );
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow(
                () -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND)
        );
    }

    // 상품이 찜 가능한 상태인지 검사
    // 상품이 삭제 됐거나 본인 상품이면 찜 불가
    // 그 외 ON_SALE / RESERVED / SOLD는 찜 가능
    private void validateProductCanBeWished(Long memberId, Product product) {
        if (product.isDeleted()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (product.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.WISH_OWN_PRODUCT_NOT_ALLOWED);
        }
    }
}
