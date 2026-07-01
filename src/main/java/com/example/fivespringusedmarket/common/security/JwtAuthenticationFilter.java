package com.example.fivespringusedmarket.common.security;

import com.example.fivespringusedmarket.auth.repository.AccessTokenBlacklistRepository;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
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
    private static final String POST_METHOD = "POST";
    private static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/reissue"
    );

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final BearerTokenResolver bearerTokenResolver;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            BearerTokenResolver bearerTokenResolver,
            AccessTokenBlacklistRepository accessTokenBlacklistRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.bearerTokenResolver = bearerTokenResolver;
        this.accessTokenBlacklistRepository = accessTokenBlacklistRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (ACTUATOR_HEALTH_PATH.equals(request.getRequestURI())) {
            return true;
        }

        return POST_METHOD.equals(request.getMethod()) && PUBLIC_AUTH_PATHS.contains(request.getRequestURI());
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
        jwtUtil.validateAccessToken(token);

        if (accessTokenBlacklistRepository.exists(token)) {
            throw new CustomException(ErrorCode.BLACKLIST_TOKEN);
        }

        AuthMember authMember = jwtUtil.extractAuthMember(token);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authMember,
                null,
                List.of(new SimpleGrantedAuthority(ROLE_PREFIX + authMember.role().name()))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
