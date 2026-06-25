package com.example.fivespringusedmarket.coupon.entity;

import com.example.fivespringusedmarket.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원에게 발급된 쿠폰 엔티티.
 * member_id + coupon_id 조합에 Unique 제약을 두어 중복 발급을 방지한다.
 */
@Getter
@Entity
@Table(
        name = "user_coupons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_coupons_member_coupon", columnNames = {"member_id", "coupon_id"}),
                @UniqueConstraint(name = "uk_user_coupons_code", columnNames = "code")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column
    private LocalDateTime usedAt;

    public boolean isUsed() {
        // usedAt 이 기록된 경우 이미 사용된 쿠폰
        return usedAt != null;
    }

    public boolean isExpired() {
        // 현재 시각이 만료일을 넘었으면 사용 불가
        return LocalDateTime.now().isAfter(expireAt);
    }

    public void use() {
        // 사용 시각을 기록해 쿠폰을 소비 상태로 전환
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 발급 시 UserCoupon 을 생성하는 정적 팩토리 메서드.
     *
     * @param member 발급 대상 회원
     * @param coupon 발급할 쿠폰
     * @param code   발급 고유 코드
     */
    public static UserCoupon issue(Member member, Coupon coupon, String code) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.member = member;
        userCoupon.coupon = coupon;
        userCoupon.code = code;
        userCoupon.issuedAt = LocalDateTime.now();
        // 쿠폰의 만료일을 그대로 상속
        userCoupon.expireAt = coupon.getExpireAt();
        return userCoupon;
    }
}
