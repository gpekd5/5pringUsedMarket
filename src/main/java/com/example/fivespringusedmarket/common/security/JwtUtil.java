package com.example.fivespringusedmarket.common.security;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.entity.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT Access Token 생성, 검증, Claim 추출을 담당한다.
 */
@Component
public class JwtUtil {

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final String secret;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:1800000}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:1209600000}") long refreshTokenExpiration
    ) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret must not be blank");
        }

        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(Member member) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        // Controller에서 AuthMember를 만들 수 있도록 필요한 최소 Claim을 담는다.
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(member.getId()))
                .claim(MEMBER_ID_CLAIM, member.getId())
                .claim(EMAIL_CLAIM, member.getEmail())
                .claim(ROLE_CLAIM, member.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public String createRefreshToken(Member member) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(member.getId()))
                .claim(MEMBER_ID_CLAIM, member.getId())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public Long extractMemberIdFromRefreshToken(String refreshToken) {
        Claims claims = parseRefreshTokenClaims(refreshToken);
        validateTokenType(claims, REFRESH_TOKEN_TYPE, ErrorCode.INVALID_REFRESH_TOKEN);

        Long memberId = claims.get(MEMBER_ID_CLAIM, Long.class);
        if (memberId == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return memberId;
    }

    public long getRemainingExpirationMillis(String accessToken) {
        Claims claims = parseAccessTokenClaims(accessToken);
        validateTokenType(claims, ACCESS_TOKEN_TYPE, ErrorCode.INVALID_TOKEN);

        Date expiration = claims.getExpiration();
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        return remainingMillis;
    }

    public void validateAccessToken(String accessToken) {
        Claims claims = parseAccessTokenClaims(accessToken);
        validateTokenType(claims, ACCESS_TOKEN_TYPE, ErrorCode.INVALID_TOKEN);
    }

    public boolean isValidToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException exception) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public AuthMember extractAuthMember(String token) {
        Claims claims = parseAccessTokenClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE, ErrorCode.INVALID_TOKEN);

        Long memberId = claims.get(MEMBER_ID_CLAIM, Long.class);
        String email = claims.get(EMAIL_CLAIM, String.class);
        String roleName = claims.get(ROLE_CLAIM, String.class);

        if (memberId == null || !StringUtils.hasText(email) || !StringUtils.hasText(roleName)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        try {
            return new AuthMember(memberId, email, MemberRole.valueOf(roleName));
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Claims parseRefreshTokenClaims(String refreshToken) {
        try {
            return parseClaims(refreshToken);
        } catch (ExpiredJwtException exception) {
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        } catch (CustomException exception) {
            if (exception.getErrorCode() == ErrorCode.EXPIRED_TOKEN) {
                throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
            }

            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private Claims parseAccessTokenClaims(String accessToken) {
        try {
            return parseClaims(accessToken);
        } catch (ExpiredJwtException exception) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateTokenType(Claims claims, String expectedTokenType, ErrorCode errorCode) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedTokenType.equals(tokenType)) {
            throw new CustomException(errorCode);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
