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

    private final String secret;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.access-token-expiration:1800000}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration:1209600000}") long refreshTokenExpiration
    ) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(Member member) {
        validateSecret();

        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        // Controller에서 AuthMember를 만들 수 있도록 필요한 최소 Claim을 담는다.
        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim(MEMBER_ID_CLAIM, member.getId())
                .claim(EMAIL_CLAIM, member.getEmail())
                .claim(ROLE_CLAIM, member.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
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
        Claims claims = parseClaims(token);

        Long memberId = claims.get(MEMBER_ID_CLAIM, Long.class);
        String email = claims.get(EMAIL_CLAIM, String.class);
        MemberRole role = MemberRole.valueOf(claims.get(ROLE_CLAIM, String.class));

        return new AuthMember(memberId, email, role);
    }

    private Claims parseClaims(String token) {
        validateSecret();

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private void validateSecret() {
        if (!StringUtils.hasText(secret)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
