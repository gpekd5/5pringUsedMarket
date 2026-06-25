package com.example.fivespringusedmarket.coupon.service;

import com.example.fivespringusedmarket.coupon.entity.Coupon;
import com.example.fivespringusedmarket.coupon.repository.CouponRepository;
import com.example.fivespringusedmarket.coupon.repository.UserCouponRepository;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:coupon-service-test;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "jwt.secret=12345678901234567890123456789012",
                "jwt.access-token-expiration=3600000"
        }
)
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 시 재고보다 많은 쿠폰이 발급되어서는 안 된다")
    void 선착순_쿠폰_동시_발급_재고_초과_불가() throws InterruptedException {
        // given
        int totalQty = 10;
        int threadCount = 100;

        Coupon coupon = createCoupon("선착순 쿠폰", totalQty);
        couponRepository.save(coupon);
        Long couponId = coupon.getId();

        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Member member = Member.create("user" + i + "@test.com", "pw", "user" + i);
            memberIds.add(memberRepository.save(member).getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final Long memberId = memberIds.get(i);
            executor.submit(() -> {
                try {
                    barrier.await(); // 모든 스레드가 여기서 대기하다 동시에 출발
                    couponService.issueCoupon(couponId, memberId);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        long issuedInDb = userCouponRepository.count();
        System.out.println("발급 성공 수: " + successCount.get() + ", DB 저장 수: " + issuedInDb);

        assertThat(issuedInDb)
                .as("실제 발급된 쿠폰 수가 재고(%d)를 초과하면 안 된다", totalQty)
                .isLessThanOrEqualTo(totalQty);
    }

    private Coupon createCoupon(String name, int totalQty) {
        Coupon coupon;
        try {
            Constructor<Coupon> constructor = Coupon.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            coupon = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(coupon, "name", name);
        ReflectionTestUtils.setField(coupon, "totalQty", totalQty);
        ReflectionTestUtils.setField(coupon, "issuedQty", 0);
        ReflectionTestUtils.setField(coupon, "eventStartAt", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(coupon, "eventEndAt", LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(coupon, "expireAt", LocalDateTime.now().plusDays(7));
        return coupon;
    }
}
