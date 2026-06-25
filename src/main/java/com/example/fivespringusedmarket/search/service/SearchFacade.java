package com.example.fivespringusedmarket.search.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.dto.ProductPageResponse;
import com.example.fivespringusedmarket.search.dto.RecentSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 검색 기능에서 다른 도메인과의 조합을 담당하는 Facade입니다.
 *
 * <p>SearchService가 MemberRepository 등에 직접 의존하지 않도록,
 * 로그인 사용자 조회 역할을 대신 수행합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class SearchFacade {

    private final SearchService searchService;
    private final MemberRepository memberRepository;

    /**
     * 상품 검색 v1을 수행합니다.
     *
     * <p>로그인 사용자인 경우 Member를 조회하여 SearchService로 전달하고,
     * 비로그인 사용자인 경우 null을 전달합니다.</p>
     */
    public ProductPageResponse searchProductsV1(Long memberId, String keyword, String category, String status, String sort, Pageable pageable)
    {

        Member member = findMemberOrNull(memberId);

        return searchService.searchProductsV1(
                member,
                keyword,
                category,
                status,
                sort,
                pageable
        );
    }

    public List<RecentSearchResponse> getRecentSearches(Long memberId) {
        return searchService.getRecentSearches(memberId);
    }

    private Member findMemberOrNull(Long memberId) {
        if (memberId == null) {
            return null;
        }

        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
