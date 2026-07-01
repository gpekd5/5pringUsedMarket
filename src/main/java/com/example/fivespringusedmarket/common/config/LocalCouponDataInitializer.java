package com.example.fivespringusedmarket.common.config;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 환경에서 쿠폰 화면 확인용 기본 데이터를 자동 생성한다.
 */
@Profile("local")
@Component
@Order(1)
public class LocalCouponDataInitializer implements ApplicationRunner {

    private static final String MEMBER_EMAIL = "member@test.com";
    private static final String MEGA_COFFEE_COUPON_NAME = "[DEMO] 메가커피 아메리카노 쿠폰";
    private static final String BAEMIN_COUPON_NAME = "[DEMO] 배달의민족 5천원 쿠폰";
    private static final String CONVENIENCE_STORE_COUPON_NAME = "[DEMO] GS25 편의점 3천원 쿠폰";

    private final JdbcTemplate jdbcTemplate;

    public LocalCouponDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        deleteLegacyDemoCoupons();

        LocalDateTime now = LocalDateTime.now();

        Long megaCoffeeCouponId = upsertCoupon(MEGA_COFFEE_COUPON_NAME, now);
        Long baeminCouponId = upsertCoupon(BAEMIN_COUPON_NAME, now);
        Long convenienceStoreCouponId = upsertCoupon(CONVENIENCE_STORE_COUPON_NAME, now);

        Long memberId = findMemberId(MEMBER_EMAIL);
        if (memberId == null) {
            return;
        }

        issueUserCouponIfNotExists(
                memberId,
                baeminCouponId,
                "DEMO-BAEMIN-0001",
                now.minusHours(2),
                now.plusDays(30),
                null
        );
        issueUserCouponIfNotExists(
                memberId,
                convenienceStoreCouponId,
                "DEMO-GS25-0001",
                now.minusDays(1),
                now.plusDays(30),
                now.minusHours(3)
        );

        syncIssuedQty(List.of(megaCoffeeCouponId, baeminCouponId, convenienceStoreCouponId));
    }

    private void deleteLegacyDemoCoupons() {
        List<String> legacyNames = List.of(
                "[DEMO] 봄맞이 거래 쿠폰",
                "[DEMO] 이미 발급받은 무료배송 쿠폰",
                "[DEMO] 사용 완료된 감사 쿠폰"
        );

        for (String legacyName : legacyNames) {
            List<Long> couponIds = jdbcTemplate.queryForList(
                    "select id from coupons where name = ?",
                    Long.class,
                    legacyName
            );

            for (Long couponId : couponIds) {
                jdbcTemplate.update("delete from user_coupons where coupon_id = ?", couponId);
                jdbcTemplate.update("delete from coupons where id = ?", couponId);
            }
        }
    }

    private Long upsertCoupon(String name, LocalDateTime now) {
        LocalDateTime eventStartAt = now.minusDays(1);
        LocalDateTime eventEndAt = now.plusDays(7);
        LocalDateTime expireAt = now.plusDays(30);

        List<Long> ids = jdbcTemplate.queryForList(
                "select id from coupons where name = ?",
                Long.class,
                name
        );

        if (ids.isEmpty()) {
            jdbcTemplate.update(
                    """
                            insert into coupons
                                (name, total_qty, issued_qty, event_start_at, event_end_at, expire_at, created_at)
                            values
                                (?, 100, 0, ?, ?, ?, ?)
                            """,
                    name,
                    eventStartAt,
                    eventEndAt,
                    expireAt,
                    now
            );

            return jdbcTemplate.queryForObject(
                    "select id from coupons where name = ?",
                    Long.class,
                    name
            );
        }

        Long id = ids.get(0);
        jdbcTemplate.update(
                """
                        update coupons
                        set total_qty = 100,
                            event_start_at = ?,
                            event_end_at = ?,
                            expire_at = ?
                        where id = ?
                        """,
                eventStartAt,
                eventEndAt,
                expireAt,
                id
        );
        return id;
    }

    private Long findMemberId(String email) {
        List<Long> ids = jdbcTemplate.queryForList(
                "select id from members where email = ?",
                Long.class,
                email
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void issueUserCouponIfNotExists(
            Long memberId,
            Long couponId,
            String code,
            LocalDateTime issuedAt,
            LocalDateTime expireAt,
            LocalDateTime usedAt
    ) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_coupons where member_id = ? and coupon_id = ?",
                Integer.class,
                memberId,
                couponId
        );

        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                        insert into user_coupons
                            (member_id, coupon_id, code, issued_at, expire_at, used_at)
                        values
                            (?, ?, ?, ?, ?, ?)
                        """,
                memberId,
                couponId,
                code,
                issuedAt,
                expireAt,
                usedAt
        );
    }

    private void syncIssuedQty(List<Long> couponIds) {
        for (Long couponId : couponIds) {
            jdbcTemplate.update(
                    """
                            update coupons
                            set issued_qty = (
                                select count(*)
                                from user_coupons
                                where coupon_id = ?
                            )
                            where id = ?
                            """,
                    couponId,
                    couponId
            );
        }
    }
}
