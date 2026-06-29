package com.example.fivespringusedmarket.wish.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.wish.dto.WishProductResponse;
import com.example.fivespringusedmarket.wish.entity.Wish;
import com.example.fivespringusedmarket.wish.repository.WishQueryRepository;
import com.example.fivespringusedmarket.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관심상품 비즈니스 로직을 처리하는 Service입니다.
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishService {

    private final WishRepository wishRepository;
    private final WishQueryRepository wishQueryRepository;
    private final S3PresignedUrlService s3PresignedUrlService;


    /**
     * 관심상품을 등록합니다.
     */
    @Transactional
    public void addWish(Member member, Product product) {
        if (wishRepository.existsByMemberIdAndProductId(member.getId(), product.getId())) {
            throw new CustomException(ErrorCode.WISH_ALREADY_EXISTS);
        }

        try {
            wishRepository.save(Wish.create(member, product));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.WISH_ALREADY_EXISTS);
        }    }

    /**
     * 관심상품을 삭제합니다.
     */
    @Transactional
    public void removeWish(Long memberId, Long productId) {
        Wish wish = wishRepository.findByMemberIdAndProductId(memberId, productId).orElseThrow(
                () -> new CustomException(ErrorCode.WISH_NOT_FOUND)
        );

        wishRepository.delete(wish);
    }

    public List<WishProductResponse> getMyWishes(Long memberId) {
        return wishQueryRepository.findMyWishes(memberId)
                .stream()
                .map(this::withPresignedThumbnailUrl)
                .toList();
    }

    /**
     * 관심상품 목록의 thumbnailUrl 자리에 들어 있던 imageKey를 Presigned URL로 바꾼다.
     */
    private WishProductResponse withPresignedThumbnailUrl(WishProductResponse wishProduct) {
        return new WishProductResponse(
                wishProduct.productId(),
                wishProduct.title(),
                wishProduct.price(),
                wishProduct.category(),
                wishProduct.status(),
                s3PresignedUrlService.createPresignedUrl(wishProduct.thumbnailUrl()),
                wishProduct.wishedAt()
        );
    }

    public long countMyWishes(Long memberId) {
        return wishRepository.countByMemberIdAndProductStatusNot(memberId, ProductStatus.DELETED);
    }

    public boolean isWished(Long memberId, Long ProductId) {
        if (memberId == null) {
            return false;
        }
        return wishRepository.existsByMemberIdAndProductId(memberId, ProductId);
    }

}
