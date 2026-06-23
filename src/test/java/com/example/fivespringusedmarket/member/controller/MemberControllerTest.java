package com.example.fivespringusedmarket.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fivespringusedmarket.common.security.JwtUtil;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:member-controller-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=12345678901234567890123456789012",
        "jwt.access-token-expiration=3600000"
})
@AutoConfigureMockMvc
class MemberControllerTest {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    void getMyInfoReturnsAuthenticatedMember() throws Exception {
        // given
        Member member = memberRepository.saveAndFlush(
                Member.create("member@test.com", "encoded-password", "일반회원")
        );
        String accessToken = jwtUtil.createAccessToken(member);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/members/me")
                .header("Authorization", BEARER_PREFIX + accessToken));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.memberId").value(member.getId()))
                .andExpect(jsonPath("$.data.email").value("member@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("일반회원"));
    }

    @Test
    void getMyInfoReturnsNotFoundWhenMemberDoesNotExist() throws Exception {
        // given
        Member member = memberRepository.saveAndFlush(
                Member.create("deleted-member@test.com", "encoded-password", "탈퇴회원")
        );
        String accessToken = jwtUtil.createAccessToken(member);
        memberRepository.deleteById(member.getId());
        memberRepository.flush();

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/members/me")
                .header("Authorization", BEARER_PREFIX + accessToken));

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."));
    }

    @Test
    void updateMyInfoChangesNicknameAndPassword() throws Exception {
        // given
        Member member = memberRepository.saveAndFlush(
                Member.create("update-member@test.com", passwordEncoder.encode("oldPassword"), "변경전")
        );
        String accessToken = jwtUtil.createAccessToken(member);
        String requestBody = """
                {
                  "nickname": "변경후",
                  "password": "newPassword"
                }
                """;

        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/members/me")
                .header("Authorization", BEARER_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 정보가 수정되었습니다."))
                .andExpect(jsonPath("$.data.memberId").value(member.getId()))
                .andExpect(jsonPath("$.data.email").value("update-member@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("변경후"));

        Optional<Member> updatedMember = memberRepository.findById(member.getId());
        assertThat(updatedMember).isPresent();
        assertThat(updatedMember.get().getNickname()).isEqualTo("변경후");
        assertThat(passwordEncoder.matches("newPassword", updatedMember.get().getPassword())).isTrue();
    }
}
