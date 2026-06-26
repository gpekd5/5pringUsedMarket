package com.example.fivespringusedmarket.coupon.repository;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * 쿠폰 발급용 Redis Lock 저장소.
 * SETNX + TTL 로 Lock 을 획득하고, Lua Script 로 본인이 획득한 Lock 만 해제한다.
 */
@Repository
@RequiredArgsConstructor
public class CouponRedisLockRepository {

    // 본인이 설정한 값과 일치할 때만 삭제하는 Lua Script (원자적 비교-삭제)
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    // 애플리케이션 시작 시 한 번만 생성 — 인스턴스 내부 SHA 캐시를 유지해
    // 두 번째 호출부터 EVAL(전체 스크립트 전송) 대신 EVALSHA(해시값만 전송)를 사용한다.
    private static final DefaultRedisScript<Long> UNLOCK_REDIS_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lock 획득 시도. 키가 없을 때만 value 를 세팅한다 (SETNX).
     *
     * @param key   Lock 키 (lock:coupon:{couponId})
     * @param value Lock 식별값 (UUID)
     * @return 획득 성공 시 true, 이미 Lock 이 존재하면 false
     */
    public Boolean lock(String key, String value) {
        // SET key value NX PX 10000 — 원자적으로 세팅, TTL 10초
        return stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, value, Duration.ofMillis(10_000));
    }

    /**
     * Lock 해제. Lua Script 로 value 가 일치하는 경우에만 삭제한다.
     *
     * @param key   Lock 키
     * @param value Lock 획득 시 사용한 UUID
     */
    public void unlock(String key, String value) {
        // 다른 스레드가 획득한 Lock 을 실수로 해제하지 않도록 value 검증 후 삭제
        // UNLOCK_REDIS_SCRIPT 는 static final — SHA 캐시 재사용으로 EVALSHA 명령 전송
        stringRedisTemplate.execute(UNLOCK_REDIS_SCRIPT, List.of(key), value);
    }
}
