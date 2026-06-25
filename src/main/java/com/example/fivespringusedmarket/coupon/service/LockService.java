package com.example.fivespringusedmarket.coupon.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.coupon.dto.IssueCouponResponse;
import com.example.fivespringusedmarket.coupon.repository.CouponRedisLockRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Redis Lock 을 이용한 쿠폰 발급 동시성 제어 서비스.
 * Lettuce SETNX 기반 Spin Lock 방식으로 구현하며, 외부에서는 이 서비스만 의존한다.
 */
@Service
@RequiredArgsConstructor
public class LockService {

    private static final String COUPON_LOCK_PREFIX = "lock:coupon:";
    private static final long SPIN_INTERVAL_MS = 100;
    private static final int MAX_RETRY = 30;

    private final CouponRedisLockRepository couponRedisLockRepository;
    private final CouponService couponService;

    /**
     * Redis Lock 을 획득한 뒤 쿠폰을 발급하고, finally 에서 반드시 Lock 을 해제한다.
     *
     * @param couponId 발급할 쿠폰 ID
     * @param memberId 발급 요청 회원 ID
     */
    public IssueCouponResponse issueCouponWithLock(Long couponId, Long memberId) {
        String lockKey = COUPON_LOCK_PREFIX + couponId;
        // UUID 로 Lock 값을 식별해 본인이 획득한 Lock 만 해제
        String lockValue = UUID.randomUUID().toString();

        try {
            acquireLock(lockKey, lockValue);
            return couponService.issueCoupon(couponId, memberId);
        } finally {
            // 예외 발생 여부와 무관하게 Lock 해제
            couponRedisLockRepository.unlock(lockKey, lockValue);
        }
    }

    private void acquireLock(String lockKey, String lockValue) {
        for (int i = 0; i < MAX_RETRY; i++) {
            Boolean acquired = couponRedisLockRepository.lock(lockKey, lockValue);
            if (Boolean.TRUE.equals(acquired)) {
                return;
            }
            try {
                // 일정 간격으로 재시도 (Spin Lock)
                Thread.sleep(SPIN_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
        // MAX_RETRY 초과 시 타임아웃 처리
        throw new CustomException(ErrorCode.COUPON_LOCK_TIMEOUT);
    }
}
