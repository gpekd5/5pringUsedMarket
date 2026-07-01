package com.example.fivespringusedmarket.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.chat.repository.ChatMemberRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.coupon.repository.UserCouponRepository;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.mypage.dto.MyPageResponse;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import com.example.fivespringusedmarket.wish.repository.WishRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishRepository wishRepository;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private MyPageService myPageService;

    @Test
    void getMyPageReturnsMemberSummaryCounts() {
        // given
        Long memberId = 1L;
        Member member = Member.create("mypage@test.com", "encoded-password", "현승");
        ReflectionTestUtils.setField(member, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(productRepository.countBySellerIdAndStatusNot(memberId, ProductStatus.DELETED)).thenReturn(3L);
        when(wishRepository.countByMemberIdAndProductStatusNot(memberId, ProductStatus.DELETED)).thenReturn(5L);
        when(chatMemberRepository.countByMemberId(memberId)).thenReturn(2L);
        when(userCouponRepository.countByMemberId(memberId)).thenReturn(1L);

        // when
        MyPageResponse response = myPageService.getMyPage(memberId);

        // then
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.nickname()).isEqualTo("현승");
        assertThat(response.sellingProductCount()).isEqualTo(3L);
        assertThat(response.wishedProductCount()).isEqualTo(5L);
        assertThat(response.chatRoomCount()).isEqualTo(2L);
        assertThat(response.couponCount()).isEqualTo(1L);

        verify(productRepository).countBySellerIdAndStatusNot(memberId, ProductStatus.DELETED);
        verify(wishRepository).countByMemberIdAndProductStatusNot(memberId, ProductStatus.DELETED);
        verify(chatMemberRepository).countByMemberId(memberId);
        verify(userCouponRepository).countByMemberId(memberId);
    }

    @Test
    void getMyPageThrowsExceptionWhenMemberDoesNotExist() {
        // given
        Long memberId = 999L;
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> myPageService.getMyPage(memberId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
