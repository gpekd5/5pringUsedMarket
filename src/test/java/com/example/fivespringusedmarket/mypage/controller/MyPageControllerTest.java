package com.example.fivespringusedmarket.mypage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fivespringusedmarket.common.security.JwtUtil;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import com.example.fivespringusedmarket.product.entity.Product;
import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import com.example.fivespringusedmarket.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mypage-controller-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=12345678901234567890123456789012",
        "jwt.access-token-expiration=1800000",
        "jwt.refresh-token-expiration=1209600000"
})
@AutoConfigureMockMvc
class MyPageControllerTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void getMyPageReturnsAuthenticatedMemberSummary() throws Exception {
        // given
        Member member = memberRepository.saveAndFlush(
                Member.create("mypage@test.com", "encoded-password", "마이페이지회원")
        );
        Product onSaleProduct = productRepository.save(
                Product.create(member, "판매중 상품", "설명", 10_000, ProductCategory.DIGITAL)
        );
        Product reservedProduct = Product.create(member, "예약 상품", "설명", 20_000, ProductCategory.BOOK);
        reservedProduct.updateStatus(ProductStatus.RESERVED);
        productRepository.saveAndFlush(reservedProduct);
        Product deletedProduct = Product.create(member, "삭제 상품", "설명", 30_000, ProductCategory.ETC);
        deletedProduct.updateStatus(ProductStatus.DELETED);
        productRepository.saveAndFlush(deletedProduct);

        String accessToken = jwtUtil.createAccessToken(member);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/mypage")
                .header("Authorization", BEARER_PREFIX + accessToken));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("마이페이지 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.memberId").value(member.getId()))
                .andExpect(jsonPath("$.data.nickname").value("마이페이지회원"))
                .andExpect(jsonPath("$.data.sellingProductCount").value(2))
                .andExpect(jsonPath("$.data.wishedProductCount").value(0))
                .andExpect(jsonPath("$.data.chatRoomCount").value(0))
                .andExpect(jsonPath("$.data.couponCount").value(0))
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    @Test
    void getMyPageWithoutAuthenticationReturnsUnauthorized() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/mypage"));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMyPageReturnsNotFoundWhenMemberDoesNotExist() throws Exception {
        // given
        Member member = memberRepository.saveAndFlush(
                Member.create("deleted-mypage@test.com", "encoded-password", "삭제회원")
        );
        String accessToken = jwtUtil.createAccessToken(member);
        memberRepository.deleteById(member.getId());
        memberRepository.flush();

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/mypage")
                .header("Authorization", BEARER_PREFIX + accessToken));

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."));
    }

    @Test
    void getMyPageKeepsWishedProductCountFieldName() throws Exception {
        // given
        Member member = memberRepository.saveAndFlush(
                Member.create("wish-field@test.com", "encoded-password", "필드명회원")
        );
        String accessToken = jwtUtil.createAccessToken(member);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/mypage")
                .header("Authorization", BEARER_PREFIX + accessToken));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wishedProductCount").value(0))
                .andExpect(jsonPath("$.data.wishProductCount").doesNotExist());
    }
}
