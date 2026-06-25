package com.example.fivespringusedmarket.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SuppressWarnings("unchecked")
class AccessTokenBlacklistRepositoryTest {

    private static final long TTL_MILLIS = 600_000L;

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final AccessTokenBlacklistRepository repository = new AccessTokenBlacklistRepository(stringRedisTemplate);

    @Test
    void saveStoresAccessTokenWithBlacklistKeyAndTtl() {
        // given
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        repository.save("access-token", TTL_MILLIS);

        // then
        verify(valueOperations).set(
                "blacklist:access-token:access-token",
                "logout",
                Duration.ofMillis(TTL_MILLIS)
        );
    }

    @Test
    void existsReturnsTrueWhenBlacklistKeyExists() {
        // given
        when(stringRedisTemplate.hasKey("blacklist:access-token:access-token")).thenReturn(true);

        // when
        boolean exists = repository.exists("access-token");

        // then
        assertThat(exists).isTrue();
    }
}
