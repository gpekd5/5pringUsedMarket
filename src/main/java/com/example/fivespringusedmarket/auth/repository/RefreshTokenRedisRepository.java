package com.example.fivespringusedmarket.auth.repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis Whitelist 방식으로 Refresh Token을 저장하고 조회한다.
 */
@Repository
public class RefreshTokenRedisRepository {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh-token:";
    private static final Long ROTATION_SUCCESS = 1L;
    private static final DefaultRedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                return 1
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenRedisRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void save(Long memberId, String refreshToken, long ttlMillis) {
        stringRedisTemplate.opsForValue()
                .set(getKey(memberId), refreshToken, Duration.ofMillis(ttlMillis));
    }

    public Optional<String> findByMemberId(Long memberId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(getKey(memberId)));
    }

    public boolean rotateIfMatches(
            Long memberId,
            String expectedRefreshToken,
            String newRefreshToken,
            long ttlMillis
    ) {
        Long result = stringRedisTemplate.execute(
                ROTATE_REFRESH_TOKEN_SCRIPT,
                List.of(getKey(memberId)),
                expectedRefreshToken,
                newRefreshToken,
                String.valueOf(ttlMillis)
        );

        return ROTATION_SUCCESS.equals(result);
    }

    public void deleteByMemberId(Long memberId) {
        stringRedisTemplate.delete(getKey(memberId));
    }

    private String getKey(Long memberId) {
        return REFRESH_TOKEN_KEY_PREFIX + memberId;
    }
}
