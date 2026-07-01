# TROUBLESHOOTING: 쿠폰 발급 Lock 우회 가능성

## 증상

`CouponService.issueCoupon()`이 `public`으로 선언되어 있고, `CouponController`가 `CouponService`와 `LockService`를 모두 주입받고 있다. 현재는 발급 엔드포인트에서 `lockService.issueCouponWithLock()`을 올바르게 호출하지만, 다른 개발자가 실수로 `couponService.issueCoupon()`을 직접 호출하면 Redis Lock 없이 쿠폰이 발급된다.

## 문제 코드

```java
// CouponController.java
private final CouponService couponService; // CouponService를 직접 주입받고 있음
private final LockService lockService;

@PostMapping("/coupons/{couponId}/issue")
public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(...) {
    // 현재는 올바르게 LockService를 통해 호출
    IssueCouponResponse result = lockService.issueCouponWithLock(couponId, memberId);

    // 하지만 아래처럼 실수할 경우 Lock 없이 실행됨 → 동시성 보호 무력화
    // IssueCouponResponse result = couponService.issueCoupon(couponId, memberId);
}
```

```java
// CouponService.java
@Transactional
public IssueCouponResponse issueCoupon(Long couponId, Long memberId) {
    // Lock 없이 직접 호출 가능한 상태
}
```

## 원인

`issueCoupon()`이 `public`이고 Controller가 `CouponService`를 직접 의존하기 때문에, Lock을 거치지 않는 호출 경로가 열려 있다. 동시 요청이 몰릴 때 Lock 없이 실행되면 수량 초과 발급, 중복 발급이 발생할 수 있다.

## 해결 방안

### 방법 1: Controller에서 CouponService 의존 제거 (선택)

쿠폰 발급 경로는 반드시 `LockService`를 통하도록 강제한다. `CouponController`에서 `CouponService` 주입을 제거하고, 발급 외 나머지 기능(`getCoupons`, `getMyCoupons`, `useCoupon`)도 `LockService`로 위임한다.

```java
// CouponController.java — CouponService 의존 제거
private final LockService lockService; // LockService만 주입
```

**방법 1을 선택한 이유**: `@Transactional` 동작을 그대로 유지하면서 Lock 우회 경로를 구조적으로 차단할 수 있다. `CouponService`의 기존 코드를 건드리지 않고 Controller의 의존 관계만 바꾸면 되므로 변경 범위가 작고 부작용이 없다.

### 방법 2: issueCoupon()을 package-private으로 변경 (미선택)

`CouponService.issueCoupon()`의 접근 제어자를 `public`에서 package-private(접근 제어자 없음)으로 변경해 같은 패키지(`coupon.service`) 외부에서 직접 호출할 수 없도록 막는다.

```java
// CouponService.java
@Transactional
IssueCouponResponse issueCoupon(Long couponId, Long memberId) { // public 제거
    ...
}
```

**방법 2를 선택하지 않은 이유**: Spring의 `@Transactional`은 AOP 프록시 방식으로 동작한다. 프록시는 외부에서 메서드를 호출할 때 트랜잭션을 시작하고 커밋하는 코드를 앞뒤로 감싸는데, 이 프록시가 생성되려면 대상 메서드가 반드시 `public`이어야 한다. `issueCoupon()`을 package-private으로 변경하면 프록시가 해당 메서드를 오버라이드할 수 없어 `@Transactional`이 무시된다. 트랜잭션 없이 실행되면 `coupon.incrementIssuedQty()`와 `userCouponRepository.save()` 중 하나가 실패해도 롤백이 되지 않아 데이터 정합성이 깨진다.

## 적용된 해결책

방법 1 적용 완료.

`LockService`에 `getCoupons`, `getMyCoupons`, `useCoupon` 위임 메서드를 추가하고, `CouponController`에서 `CouponService` 주입을 제거했다. `CouponController`는 `LockService`만 의존하므로 `CouponService.issueCoupon()`을 직접 호출하는 경로가 구조적으로 차단된다.

```java
// LockService.java — 위임 메서드 추가
public Page<CouponResponse> getCoupons(Pageable pageable) {
    return couponService.getCoupons(pageable);
}

public Page<UserCouponResponse> getMyCoupons(Long memberId, Boolean used, Pageable pageable) {
    return couponService.getMyCoupons(memberId, used, pageable);
}

public void useCoupon(Long userCouponId, Long memberId) {
    couponService.useCoupon(userCouponId, memberId);
}
```

```java
// CouponController.java — CouponService 의존 제거
private final LockService lockService; // LockService만 주입
```

## 관련 파일

- [`coupon/controller/CouponController.java`](../src/main/java/com/example/fivespringusedmarket/coupon/controller/CouponController.java)
- [`coupon/service/CouponService.java`](../src/main/java/com/example/fivespringusedmarket/coupon/service/CouponService.java)
- [`coupon/service/LockService.java`](../src/main/java/com/example/fivespringusedmarket/coupon/service/LockService.java)
