package com.example.fivespringusedmarket.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Authorization Header에서 Bearer Token을 추출한다.
 */
@Component
public class BearerTokenResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public String resolve(HttpServletRequest request) {
        return resolve(request.getHeader(AUTHORIZATION_HEADER));
    }

    public String resolve(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
