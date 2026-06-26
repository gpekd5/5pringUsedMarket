# 캐시 구현 방식 비교 (Map vs Spring Cache vs Caffeine)

## 1. 목적

검색 기능은 동일한 조건으로 반복 조회되는 경우가 많다.

예를 들어 사용자가 아래와 같이 검색했다고 가정해보자.

```
검색어 : 아이폰
카테고리 : 전자기기
상태 : 판매중
정렬 : 최신순
페이지 : 0
```

잠시 후 다른 사용자가 동일한 조건으로 검색하면 **DB는 동일한 결과를 다시 조회**하게 된다.

이러한 반복적인 조회는

* DB 부하 증가
* 응답 속도 저하
* 불필요한 SQL 실행

이라는 문제를 발생시킨다.

이를 해결하기 위해 **조회 결과를 일정 시간 메모리에 저장해두고 재사용하는 기술이 캐시(Cache)** 이다.

이번 문서에서는 검색 결과 캐시를 구현하는 대표적인 세 가지 방식을 비교한다.

* 직접 Map 사용
* Spring Cache + Simple Cache
* Spring Cache + Caffeine

---

# 2. 직접 Map 사용

## 개념

가장 기본적인 캐시 구현 방식이다.

개발자가 직접 `ConcurrentHashMap`을 생성하여

* 조회
* 저장
* 삭제
* 만료

등 모든 기능을 직접 구현한다.

Spring의 도움을 전혀 받지 않는 순수 Java 방식이다.

---

## 구현 흐름

```
검색 요청
      │
      ▼
검색 조건으로 Cache Key 생성
      │
      ▼
ConcurrentHashMap.get(key)
      │
 ┌────┴────┐
 │         │
Hit       Miss
 │         │
 ▼         ▼
반환     DB 조회
            │
            ▼
      cache.put()
            │
            ▼
          반환
```

---

## 예시 코드

```java
private final Map<String, Object> cache = new ConcurrentHashMap<>();

public Object search(String keyword) {

    Object cached = cache.get(keyword);

    if (cached != null) {
        return cached;
    }

    Object result = repository.search(keyword);

    cache.put(keyword, result);

    return result;
}
```

---

## 개발자가 직접 구현해야 하는 기능

### 1. Cache Key 생성

동일한 검색 조건이면 항상 같은 Key가 생성되어야 한다.

```
keyword=아이폰
category=전자기기
page=0

↓

아이폰:전자기기:0
```

---

### 2. 조회

```java
cache.get(key);
```

---

### 3. 저장

```java
cache.put(key, result);
```

---

### 4. TTL(Time To Live)

몇 분 뒤 캐시를 삭제할지 직접 구현해야 한다.

예)

```
현재시간
14:00

↓

5분 후 만료

↓

14:05
```

---

### 5. 만료 확인

```java
if(cache.isExpired()){
    cache.remove(key);
}
```

---

### 6. 최대 개수 제한

```
최대 500개까지만 저장
```

이런 정책도 직접 구현해야 한다.

---

## 장점

* 캐시의 원리를 가장 잘 이해할 수 있다.
* Spring 없이도 구현 가능하다.
* 원하는 방식으로 자유롭게 구현할 수 있다.

---

## 단점

모든 기능을 개발자가 직접 구현해야 한다.

* TTL
* 삭제 정책
* 최대 크기
* 메모리 관리
* 동시성 고려

실무에서는 유지보수 비용이 매우 크다.

---

# 3. Spring Cache + Simple Cache

## 개념

Spring이 제공하는 **Cache 추상화(Cache Abstraction)** 를 사용하는 방식이다.

개발자는

```java
@Cacheable
```

만 붙이면 된다.

캐시 조회 및 저장은 Spring이 자동으로 처리한다.

---

## Spring Cache란?

Spring Cache는

"캐시를 사용하는 방법"

만 정의한 것이다.

즉,

```
캐시를 어떻게 조회할지

↓

캐시에 어떻게 저장할지

↓

언제 메서드를 실행할지
```

를 Spring이 대신 처리한다.

하지만

> 실제 데이터를 저장하는 공간은 없다.

그래서 저장소가 필요하다.

Spring Boot에서 아무것도 설정하지 않으면

기본 저장소인 **Simple Cache**가 사용된다.

---

## Simple Cache란?

Simple Cache는

```
ConcurrentHashMap
```

기반의 가장 단순한 캐시 구현체이다.

즉,

```
Spring Cache

↓

Simple Cache(Map)
```

구조이다.

---

## 구현 흐름

```
검색 요청
      │
      ▼
@Cacheable
      │
      ▼
Spring AOP
      │
      ▼
Cache 조회
      │
 ┌────┴────┐
 │         │
Hit       Miss
 │         │
 ▼         ▼
반환     메서드 실행
            │
            ▼
         DB 조회
            │
            ▼
      캐시에 자동 저장
```

---

## 코드

```java
@EnableCaching
@Configuration
public class CacheConfig {
}
```

```java
@Cacheable(
        cacheNames = "productSearch",
        key = "#keyword"
)
public Object search(String keyword){

    return repository.search(keyword);

}
```

---

## 중요한 점

캐시에 값이 존재하면

아래 코드가 **실행되지 않는다.**

```java
return repository.search(keyword);
```

Spring이 메서드 실행 자체를 생략한다.

---

## 장점

* 코드가 매우 간단하다.
* get(), put()을 직접 작성하지 않는다.
* Spring 방식으로 캐시를 사용할 수 있다.

---

## 단점

기본 구현체(Simple Cache)는

* TTL 없음
* 최대 개수 제한 없음
* 오래된 캐시 자동 삭제 없음

즉

정책이 거의 없는 단순 Map이다.

---

# 4. Spring Cache + Caffeine

## 개념

Spring Cache는 그대로 사용하면서

저장소만

```
Simple Cache

↓

Caffeine
```

으로 교체하는 방식이다.

즉,

Spring Cache의 장점은 그대로 유지하면서

고급 캐시 기능을 사용할 수 있다.

---

## 구조

```
검색 요청

↓

@Cacheable

↓

Spring Cache

↓

Caffeine

↓

DB
```

---

## CacheManager 설정

```java
@Bean
public CacheManager cacheManager(){

    CaffeineCacheManager manager =
            new CaffeineCacheManager("productSearch");

    manager.setCaffeine(
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .maximumSize(500)
    );

    return manager;

}
```

---

## Service

```java
@Cacheable(
        cacheNames = "productSearch",
        key = "#keyword"
)
public Object search(String keyword){

    return repository.search(keyword);

}
```

---

## Caffeine이 자동으로 해주는 기능

### TTL

```
5분 후 자동 삭제
```

---

### Maximum Size

```
최대 500개 저장
```

---

### 자동 제거 정책

사용하지 않는 캐시는 자동으로 제거한다.

---

### 성능 최적화

LRU 등에 기반한 효율적인 메모리 관리가 가능하다.

---

## 장점

* Spring Cache 그대로 사용
* TTL 지원
* 최대 크기 제한
* 자동 삭제
* 매우 빠른 성능

실무에서도 많이 사용하는 로컬 캐시이다.

---

## 단점

메모리 캐시이므로

서버가 여러 대인 경우

```
A 서버 캐시

≠

B 서버 캐시
```

가 된다.

즉

분산 캐시는 아니다.

---

# 5. 핵심 차이

| 구분           | 직접 Map | Spring + Simple | Spring + Caffeine |
| ------------ | ------ | --------------- | ----------------- |
| 캐시 조회        | 직접 구현  | Spring          | Spring            |
| 캐시 저장        | 직접 구현  | Spring          | Spring            |
| Cache Key 생성 | 직접 구현  | 직접 지정           | 직접 지정             |
| TTL          | 직접 구현  | 기본 제공 X         | O                 |
| 최대 개수        | 직접 구현  | 기본 제공 X         | O                 |
| 삭제 정책        | 직접 구현  | 기본 제공 X         | O                 |
| 코드량          | 많음     | 적음              | 적음                |
| 유지보수         | 어려움    | 보통              | 쉬움                |
| 학습 목적        | 캐시 원리  | Spring Cache 이해 | 실무 적용             |

---

# 6. 이번 프로젝트에서는 왜 Caffeine을 선택했는가?

검색 기능은

* 동일한 조회가 많다.
* 실시간성이 매우 높지 않다.
* 단일 서버 환경이다.

따라서

Redis처럼 별도 인프라를 구축하기보다

Spring Cache + Caffeine을 사용하면

* 코드가 간단하고
* 성능도 좋으며
* TTL도 사용할 수 있어

현재 프로젝트 규모에 가장 적합하다고 판단하였다.

---

# 7. 정리

캐시는 "조회 결과를 재사용하여 DB 부하를 줄이는 기술"이다.

각 방식은 다음과 같이 이해하면 된다.

* **직접 Map** : 캐시 원리를 이해하기 위한 가장 기본적인 구현 방식
* **Spring Cache + Simple Cache** : Spring이 캐시 조회/저장을 대신 처리하는 기본 방식
* **Spring Cache + Caffeine** : Spring Cache를 유지하면서 TTL, 최대 크기 제한 등 실무에서 필요한 기능을 제공하는 방식

이번 프로젝트에서는 단일 서버 환경에서 검색 성능을 개선하기 위해 **Spring Cache + Caffeine** 방식을 적용하였다.
