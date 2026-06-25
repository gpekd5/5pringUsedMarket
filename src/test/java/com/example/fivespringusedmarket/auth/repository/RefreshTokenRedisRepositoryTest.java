package com.example.fivespringusedmarket.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings({"unchecked", "rawtypes"})
class RefreshTokenRedisRepositoryTest {

    private static final Long MEMBER_ID = 1L;
    private static final long TTL_MILLIS = 1_209_600_000L;

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RefreshTokenRedisRepository repository = new RefreshTokenRedisRepository(stringRedisTemplate);

    @Test
    void saveStoresRefreshTokenWithMemberKeyAndTtl() {
        // given
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        repository.save(MEMBER_ID, "refresh-token", TTL_MILLIS);

        // then
        verify(valueOperations).set("refresh-token:1", "refresh-token", Duration.ofMillis(TTL_MILLIS));
    }

    @Test
    void findByMemberIdReturnsStoredRefreshToken() {
        // given
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh-token:1")).thenReturn("refresh-token");

        // when
        Optional<String> refreshToken = repository.findByMemberId(MEMBER_ID);

        // then
        assertThat(refreshToken).contains("refresh-token");
    }

    @Test
    void rotateIfMatchesReturnsTrueWhenRedisScriptSucceeds() {
        // given
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("refresh-token:1")),
                eq("old-refresh-token"),
                eq("new-refresh-token"),
                eq(String.valueOf(TTL_MILLIS))
        )).thenReturn(1L);

        // when
        boolean rotated = repository.rotateIfMatches(
                MEMBER_ID,
                "old-refresh-token",
                "new-refresh-token",
                TTL_MILLIS
        );

        // then
        assertThat(rotated).isTrue();
    }

    @Test
    void rotateIfMatchesReturnsFalseWhenRedisScriptFails() {
        // given
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("refresh-token:1")),
                eq("old-refresh-token"),
                eq("new-refresh-token"),
                eq(String.valueOf(TTL_MILLIS))
        )).thenReturn(0L);

        // when
        boolean rotated = repository.rotateIfMatches(
                MEMBER_ID,
                "old-refresh-token",
                "new-refresh-token",
                TTL_MILLIS
        );

        // then
        assertThat(rotated).isFalse();
    }
}
