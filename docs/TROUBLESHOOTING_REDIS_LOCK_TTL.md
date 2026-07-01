# 트러블슈팅: Redis Lock TTL ≤ 최대 스핀 대기 시간

## 문제 요약

Lock TTL이 최대 스핀 대기 시간과 같거나 짧을 경우, 마지막 재시도에서 Lock을 획득해도 TTL이 이미 만료되어 **Lock 없이 비즈니스 로직이 실행**될 수 있다.

---

## 발생 원인

### 초기 설정값

```java
// CouponRedisLockRepository.java
public Boolean lock(String key, String value) {
    return stringRedisTemplate
            .opsForValue()
            .setIfAbsent(key, value, Duration.ofMillis(3_000)); // TTL = 3초
}

// LockService.java
private static final long SPIN_INTERVAL_MS = 100;  // 재시도 간격
private static final int  MAX_RETRY        = 30;   // 최대 재시도 횟수
```

### 타임라인 분석

```
경과 시간(ms)   이벤트
────────────────────────────────────────
0              스레드 A → Lock 획득, TTL 3,000ms 시작
100            스레드 B → Lock 획득 실패 (1회차 재시도)
200            스레드 B → Lock 획득 실패 (2회차 재시도)
...
2,900          스레드 B → Lock 획득 실패 (29회차 재시도)
3,000          ※ TTL 만료 → Redis에서 키 자동 삭제
3,000          스레드 B → Lock 획득 성공 (30회차 = 마지막)
               → setIfAbsent() 성공, 새 TTL 3,000ms 시작
3,000~         스레드 B → DB 조회 4회 + 트랜잭션 커밋 시작
               ↕ 소요 시간이 3,000ms를 초과하면?
6,000+         ※ 새 TTL 만료 → 비즈니스 로직이 아직 실행 중
               → 다른 스레드가 Lock 획득 가능 (동시성 보호 무력화)
```

### 핵심 문제

```
최대 스핀 대기 = SPIN_INTERVAL_MS × MAX_RETRY = 100ms × 30 = 3,000ms
Lock TTL      = 3,000ms

TTL ≤ 최대 스핀 대기
→ Lock을 획득한 시점에 TTL 잔여 시간이 0ms에 가까울 수 있음
→ 비즈니스 로직 실행 도중 TTL 만료 → 다른 스레드 Lock 획득 가능
```

---

## 해결 방법

TTL을 최대 스핀 대기보다 **충분히 크게** 설정한다.

```
TTL > 최대 스핀 대기 + 비즈니스 로직 최대 실행 시간
```

### 수정 코드

```java
// CouponRedisLockRepository.java
public Boolean lock(String key, String value) {
    // TTL 10,000ms > 최대 스핀 대기 3,000ms → 최소 7초 여유 확보
    return stringRedisTemplate
            .opsForValue()
            .setIfAbsent(key, value, Duration.ofMillis(10_000));
}
```

### 수정 후 타임라인

```
경과 시간(ms)   이벤트
────────────────────────────────────────
0              스레드 A → Lock 획득, TTL 10,000ms 시작
...
2,900          스레드 B → 마지막(30회차) 재시도 직전
3,000          스레드 B → Lock 획득 성공
               → TTL 잔여: 10,000ms (새 TTL 시작)
3,000~         스레드 B → DB 조회 + 트랜잭션 커밋 (~수백 ms)
               → TTL 만료까지 ~9,700ms 여유
10,000+        TTL 만료 (비즈니스 로직은 이미 완료 상태)
```

---

## 설정값 선택 기준

| 항목 | 값 | 근거 |
|------|-----|------|
| `SPIN_INTERVAL_MS` | 100ms | 짧은 간격으로 빠른 Lock 획득 시도 |
| `MAX_RETRY` | 30회 | 최대 3초 대기 (100ms × 30) |
| 최대 스핀 대기 | 3,000ms | SPIN_INTERVAL_MS × MAX_RETRY |
| Lock TTL | **10,000ms** | 최대 스핀 대기(3,000ms) + 비즈니스 로직 여유(7,000ms) |

> **규칙**: `TTL ≥ 최대 스핀 대기 × 3` 이상으로 설정하는 것을 권장한다.

---

## 관련 파일

- [`coupon/repository/CouponRedisLockRepository.java`](../src/main/java/com/example/fivespringusedmarket/coupon/repository/CouponRedisLockRepository.java) — TTL 설정
- [`coupon/service/LockService.java`](../src/main/java/com/example/fivespringusedmarket/coupon/service/LockService.java) — SPIN_INTERVAL_MS, MAX_RETRY 설정
