package com.example.fivespringusedmarket.coupon.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.coupon.dto.IssueCouponResponse;
import com.example.fivespringusedmarket.coupon.repository.CouponRedisLockRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LockService {

    private static final String COUPON_LOCK_PREFIX = "lock:coupon:";
    private static final long SPIN_INTERVAL_MS = 100;
    private static final int MAX_RETRY = 30;

    private final CouponRedisLockRepository couponRedisLockRepository;
    private final CouponService couponService;

    public IssueCouponResponse issueCouponWithLock(Long couponId, Long memberId) {
        String lockKey = COUPON_LOCK_PREFIX + couponId;
        String lockValue = UUID.randomUUID().toString();

        try {
            acquireLock(lockKey, lockValue);
            return couponService.issueCoupon(couponId, memberId);
        } finally {
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
                Thread.sleep(SPIN_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
        throw new CustomException(ErrorCode.COUPON_LOCK_TIMEOUT);
    }
}
