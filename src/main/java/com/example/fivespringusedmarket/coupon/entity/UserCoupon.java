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

    public static UserCoupon issue(Member member, Coupon coupon, String code) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.member = member;
        userCoupon.coupon = coupon;
        userCoupon.code = code;
        userCoupon.issuedAt = LocalDateTime.now();
        userCoupon.expireAt = coupon.getExpireAt();
        return userCoupon;
    }
}
