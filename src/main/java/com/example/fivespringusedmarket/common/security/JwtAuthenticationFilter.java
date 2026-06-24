package com.example.fivespringusedmarket.common.security;

import com.example.fivespringusedmarket.common.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization Header의 Bearer Token을 검증하고 인증 정보를 SecurityContext에 저장한다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final BearerTokenResolver bearerTokenResolver;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            BearerTokenResolver bearerTokenResolver
    ) {
        this.jwtUtil = jwtUtil;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = bearerTokenResolver.resolve(request);

        try {
            if (StringUtils.hasText(token)) {
                authenticate(token);
            }

            filterChain.doFilter(request, response);
        } catch (CustomException exception) {
            SecurityContextHolder.clearContext();
            jwtAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(exception.getErrorCode().getMessage(), exception)
            );
        }
    }

    private void authenticate(String token) {
        jwtUtil.isValidToken(token);
        AuthMember authMember = jwtUtil.extractAuthMember(token);

        // 추후 로그아웃 구현 시 이 위치에 Access Token Blacklist 검증을 추가한다.
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authMember,
                null,
                List.of(new SimpleGrantedAuthority(ROLE_PREFIX + authMember.role().name()))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
