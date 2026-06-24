package com.example.fivespringusedmarket.auth.service;

import com.example.fivespringusedmarket.auth.dto.LoginRequest;
import com.example.fivespringusedmarket.auth.dto.SignupRequest;
import com.example.fivespringusedmarket.auth.dto.SignupResponse;
import com.example.fivespringusedmarket.auth.dto.ReissueRequest;
import com.example.fivespringusedmarket.auth.dto.TokenResponse;
import com.example.fivespringusedmarket.auth.repository.AccessTokenBlacklistRepository;
import com.example.fivespringusedmarket.auth.repository.RefreshTokenRedisRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.common.security.JwtUtil;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 회원가입, 로그인, Access Token 발급을 담당한다.
 */
@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RefreshTokenRedisRepository refreshTokenRedisRepository,
            AccessTokenBlacklistRepository accessTokenBlacklistRepository
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
        this.accessTokenBlacklistRepository = accessTokenBlacklistRepository;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateDuplicatedEmail(request.email());
        validateDuplicatedNickname(request.nickname());

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = Member.create(request.email(), encodedPassword, request.nickname());
        Member savedMember = saveMember(member);

        return new SignupResponse(savedMember.getId(), savedMember.getEmail(), savedMember.getNickname());
    }

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtUtil.createAccessToken(member);
        String refreshToken = jwtUtil.createRefreshToken(member);
        refreshTokenRedisRepository.save(member.getId(), refreshToken, jwtUtil.getRefreshTokenExpiration());

        return TokenResponse.from(accessToken, refreshToken);
    }

    public TokenResponse reissue(ReissueRequest request) {
        String requestedRefreshToken = request.refreshToken();
        Long memberId = jwtUtil.extractMemberIdFromRefreshToken(requestedRefreshToken);

        String savedRefreshToken = refreshTokenRedisRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!savedRefreshToken.equals(requestedRefreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = jwtUtil.createAccessToken(member);
        String newRefreshToken = jwtUtil.createRefreshToken(member);
        refreshTokenRedisRepository.save(member.getId(), newRefreshToken, jwtUtil.getRefreshTokenExpiration());

        return TokenResponse.from(newAccessToken, newRefreshToken);
    }

    public void logout(Long memberId, String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        long remainingExpirationMillis = jwtUtil.getRemainingExpirationMillis(accessToken);
        accessTokenBlacklistRepository.save(accessToken, remainingExpirationMillis);
        refreshTokenRedisRepository.deleteByMemberId(memberId);
    }

    private void validateDuplicatedEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATED_EMAIL);
        }
    }

    private void validateDuplicatedNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATED_NICKNAME);
        }
    }

    private Member saveMember(Member member) {
        try {
            // 동시 회원가입 요청 시 DB unique 제약 위반을 서비스 예외로 변환하기 위해 즉시 flush한다.
            return memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw resolveDuplicatedMemberException(exception);
        }
    }

    private CustomException resolveDuplicatedMemberException(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        String lowerCaseMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (lowerCaseMessage.contains("uk_member_nickname") || lowerCaseMessage.contains("nickname")) {
            return new CustomException(ErrorCode.DUPLICATED_NICKNAME);
        }

        if (lowerCaseMessage.contains("uk_member_email") || lowerCaseMessage.contains("email")) {
            return new CustomException(ErrorCode.DUPLICATED_EMAIL);
        }

        return new CustomException(ErrorCode.INVALID_REQUEST);
    }
}
