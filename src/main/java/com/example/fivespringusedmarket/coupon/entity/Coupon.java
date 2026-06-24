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
        return now.isBefore(eventStartAt);
    }

    public boolean isEventEnded(LocalDateTime now) {
        return now.isAfter(eventEndAt);
    }

    public boolean hasStock() {
        return issuedQty < totalQty;
    }

    public void incrementIssuedQty() {
        this.issuedQty++;
    }
}
