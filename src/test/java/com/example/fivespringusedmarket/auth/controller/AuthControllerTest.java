package com.example.fivespringusedmarket.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fivespringusedmarket.auth.repository.AccessTokenBlacklistRepository;
import com.example.fivespringusedmarket.auth.repository.RefreshTokenRedisRepository;
import com.example.fivespringusedmarket.common.security.JwtUtil;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-controller-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=12345678901234567890123456789012",
        "jwt.access-token-expiration=1800000",
        "jwt.refresh-token-expiration=1209600000"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final long REFRESH_TOKEN_EXPIRATION = 1_209_600_000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Autowired
    private AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        reset(refreshTokenRedisRepository, accessTokenBlacklistRepository);
        when(accessTokenBlacklistRepository.exists(anyString())).thenReturn(false);
    }

    @Test
    void loginIssuesAccessTokenAndRefreshTokenAndStoresRefreshToken() throws Exception {
        // given
        Member member = saveMember("login@test.com", "Password123!", "로그인회원");
        String requestBody = """
                {
                  "email": "login@test.com",
                  "password": "Password123!"
                }
                """;

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

        ArgumentCaptor<String> refreshTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRedisRepository).save(
                eq(member.getId()),
                refreshTokenCaptor.capture(),
                eq(REFRESH_TOKEN_EXPIRATION)
        );
        assertThat(jwtUtil.extractMemberIdFromRefreshToken(refreshTokenCaptor.getValue())).isEqualTo(member.getId());
    }

    @Test
    void reissueRotatesRefreshTokenWhenStoredTokenMatches() throws Exception {
        // given
        Member member = saveMember("reissue@test.com", "Password123!", "재발급회원");
        String oldRefreshToken = jwtUtil.createRefreshToken(member);
        when(refreshTokenRedisRepository.findByMemberId(member.getId())).thenReturn(Optional.of(oldRefreshToken));
        Thread.sleep(5);

        String requestBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(oldRefreshToken);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

        ArgumentCaptor<String> newRefreshTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRedisRepository).save(
                eq(member.getId()),
                newRefreshTokenCaptor.capture(),
                eq(REFRESH_TOKEN_EXPIRATION)
        );
        assertThat(newRefreshTokenCaptor.getValue()).isNotEqualTo(oldRefreshToken);
    }

    @Test
    void reissueFailsWhenRefreshTokenDoesNotMatchStoredToken() throws Exception {
        // given
        Member member = saveMember("mismatch@test.com", "Password123!", "불일치회원");
        String requestedRefreshToken = jwtUtil.createRefreshToken(member);
        when(refreshTokenRedisRepository.findByMemberId(member.getId())).thenReturn(Optional.of("different-token"));

        String requestBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(requestedRefreshToken);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutStoresAccessTokenBlacklistAndDeletesRefreshToken() throws Exception {
        // given
        Member member = saveMember("logout@test.com", "Password123!", "로그아웃회원");
        String accessToken = jwtUtil.createAccessToken(member);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", BEARER_PREFIX + accessToken));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(accessTokenBlacklistRepository).save(eq(accessToken), longThat(ttl -> ttl > 0));
        verify(refreshTokenRedisRepository).deleteByMemberId(member.getId());
    }

    @Test
    void blacklistedAccessTokenIsRejectedByAuthenticationFilter() throws Exception {
        // given
        Member member = saveMember("blacklist@test.com", "Password123!", "블랙리스트회원");
        String accessToken = jwtUtil.createAccessToken(member);
        when(accessTokenBlacklistRepository.exists(accessToken)).thenReturn(true);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/members/me")
                .header("Authorization", BEARER_PREFIX + accessToken));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BLACKLIST_TOKEN"));
    }

    private Member saveMember(String email, String rawPassword, String nickname) {
        return memberRepository.saveAndFlush(
                Member.create(email, passwordEncoder.encode(rawPassword), nickname)
        );
    }

    @TestConfiguration
    static class RedisRepositoryTestConfig {

        @Bean
        @Primary
        RefreshTokenRedisRepository refreshTokenRedisRepository() {
            return mock(RefreshTokenRedisRepository.class);
        }

        @Bean
        @Primary
        AccessTokenBlacklistRepository accessTokenBlacklistRepository() {
            return mock(AccessTokenBlacklistRepository.class);
        }
    }
}
