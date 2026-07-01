package com.example.fivespringusedmarket.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtUtilTest {

    @Test
    void constructorRejectsBlankSecret() {
        assertThatThrownBy(() -> new JwtUtil(" ", 1800000L, 1209600000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT secret must not be blank");
    }
}
