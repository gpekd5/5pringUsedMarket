package com.example.fivespringusedmarket.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 선착순 이벤트 쿠폰 엔티티.
 * 이벤트 기간과 수량을 관리하며, 발급 가능 여부 판단 로직을 포함한다.
 */
@Getter
@Entity
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private int totalQty;

    @Column(nullable = false)
    private int issuedQty;

    @Column(nullable = false)
    private LocalDateTime eventStartAt;

    @Column(nullable = false)
    private LocalDateTime eventEndAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isEventNotStarted(LocalDateTime now) {
        // 현재 시각이 이벤트 시작 전이면 발급 불가
        return now.isBefore(eventStartAt);
    }

    public boolean isEventEnded(LocalDateTime now) {
        // 현재 시각이 이벤트 종료 후이면 발급 불가
        return now.isAfter(eventEndAt);
    }

    public boolean hasStock() {
        // 발급된 수량이 총 수량 미만이어야 재고 있음
        return issuedQty < totalQty;
    }

    public void incrementIssuedQty() {
        // Redis Lock 보호 하에 호출되므로 동시성 충돌 없이 안전하게 증가
        this.issuedQty++;
    }
}
