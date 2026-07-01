# 트러블슈팅: DefaultRedisScript 매 호출마다 new 생성

## 문제 요약

`unlock()` 호출마다 `DefaultRedisScript`를 새로 생성하면 인스턴스 내부 SHA 캐시가 버려져,
매번 Lua 스크립트 전체를 Redis로 전송하는 `EVAL` 명령을 반복하게 된다.

---

## 발생 원인

### 문제 코드

```java
public void unlock(String key, String value) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class); // 매 호출마다 new
    stringRedisTemplate.execute(script, List.of(key), value);
}
```

### EVAL vs EVALSHA

Redis는 Lua 스크립트를 실행하는 두 가지 명령어를 제공한다.

| 명령어 | 전송 내용 | 특징 |
|--------|-----------|------|
| `EVAL` | 스크립트 전체 문자열 | 매번 스크립트를 파싱 및 컴파일 |
| `EVALSHA` | SHA1 해시값(40자) | 서버에 캐싱된 스크립트를 해시로 참조 |

`DefaultRedisScript`는 내부적으로 스크립트를 Redis 서버에 처음 올릴 때 SHA1 해시를 발급받아 캐싱한 뒤,
이후 호출부터는 `EVALSHA`로 전환하는 최적화를 제공한다.

### SHA 캐시가 버려지는 흐름

```
unlock() 1회차 호출
  └─ new DefaultRedisScript() → SHA 캐시 없음 → EVAL (스크립트 전체 전송)
     → Redis 서버: 스크립트 파싱 + SHA 발급 + 실행
     → 인스턴스 내부에 SHA 저장
     → 메서드 종료 시 인스턴스 GC 수거 → SHA 캐시 소멸

unlock() 2회차 호출
  └─ new DefaultRedisScript() → SHA 캐시 없음 → 다시 EVAL (스크립트 전체 전송)
     → 1회차와 동일 반복 ...
```

동시 발급 1,000건이면 `DefaultRedisScript` 객체 1,000개 생성 + `EVAL` 1,000회 반복.

---

## 해결 방법

`static final` 필드로 선언해 애플리케이션 시작 시 한 번만 생성한다.

### 수정 코드

```java
// 애플리케이션 시작 시 한 번만 생성 — 인스턴스 내부 SHA 캐시를 유지해
// 두 번째 호출부터 EVAL(전체 스크립트 전송) 대신 EVALSHA(해시값만 전송)를 사용한다.
private static final DefaultRedisScript<Long> UNLOCK_REDIS_SCRIPT =
        new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

public void unlock(String key, String value) {
    // UNLOCK_REDIS_SCRIPT 는 static final — SHA 캐시 재사용으로 EVALSHA 명령 전송
    stringRedisTemplate.execute(UNLOCK_REDIS_SCRIPT, List.of(key), value);
}
```

### 수정 후 흐름

```
unlock() 1회차 호출
  └─ UNLOCK_REDIS_SCRIPT (static) → SHA 캐시 없음 → EVAL (스크립트 전체 전송)
     → Redis 서버: 스크립트 파싱 + SHA 발급 + 실행
     → static 필드 내부에 SHA 저장 (앱 종료 전까지 유지)

unlock() 2회차 이후
  └─ UNLOCK_REDIS_SCRIPT (static) → SHA 캐시 있음 → EVALSHA (해시값만 전송)
     → Redis 서버: SHA로 캐싱된 스크립트 바로 실행
```

---

## 개선 효과

| | 수정 전 | 수정 후 |
|---|---|---|
| 객체 생성 횟수 | 호출마다 1개 | 앱 시작 시 1개 |
| Redis 명령 | 매번 `EVAL` (전체 스크립트 전송) | 첫 번째만 `EVAL`, 이후 `EVALSHA` |
| GC 부담 | 있음 | 없음 |

---

## 관련 파일

- [`coupon/repository/CouponRedisLockRepository.java`](../../src/main/java/com/example/fivespringusedmarket/coupon/repository/CouponRedisLockRepository.java) — `UNLOCK_REDIS_SCRIPT` static final 선언
