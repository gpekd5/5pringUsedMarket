# [Troubleshooting] 검색 V2에서 캐시 적용 시 검색 기록이 저장되지 않는 문제

## 1. 배경

검색 V2 API에서는 동일한 검색 조건의 반복 요청에 대해 DB 조회를 줄이기 위해 Caffeine Local Cache를 적용했다.

Spring Cache의 `@Cacheable`을 사용하면 캐시에 동일한 key가 존재하는 경우 메서드 실행을 생략하고 캐시된 값을 바로 반환할 수 있다.

처음에는 검색 V2 메서드 전체에 `@Cacheable`을 적용하는 방식을 고려했다.

```java
@Cacheable(...)
public ProductPageResponse searchProductsV2(...) {
    saveSearchLog(member, keyword);
    Page<ProductListItemResponse> products = productSearchRepository.search(condition, pageable);
    return ProductPageResponse.of(products);
}
```

하지만 검색 API는 단순히 상품 목록만 조회하는 기능이 아니었다.

```text
1. 검색 조건 파싱
2. 로그인 사용자의 최근 검색어 저장
3. Redis ZSet 인기검색어 점수 증가
4. 상품 목록 조회
5. 응답 조립
```

---

## 2. 문제 상황

`@Cacheable`은 캐시 Hit가 발생하면 메서드 내부를 실행하지 않는다.

```text
검색 요청
→ Spring Cache가 key 확인
→ 캐시 Hit
→ searchProductsV2() 메서드 실행 생략
→ 캐시 결과 반환
```

이 경우 상품 목록 조회가 생략되는 것은 의도한 동작이다.

하지만 문제는 검색 로그 저장 로직도 함께 생략된다는 점이다.

```text
캐시 Hit
→ 메서드 실행 안 됨
→ saveSearchLog() 실행 안 됨
→ 최근 검색어 저장 안 됨
→ 인기검색어 집계 안 됨
```

검색 결과는 캐시를 사용할 수 있지만, 사용자가 검색했다는 행위 자체는 캐시 Hit 여부와 관계없이 매번 기록되어야 한다.

따라서 검색 V2 메서드 전체에 `@Cacheable`을 적용하는 것은 적절하지 않다고 판단했다.

---

## 3. 원인 분석

문제의 원인은 `@Cacheable`의 동작 방식에 있었다.

Spring Cache는 기본적으로 Spring AOP 프록시 기반으로 동작한다.

```text
외부 호출
→ Spring Proxy
→ Cache 확인
→ Hit면 실제 메서드 실행 생략
→ Miss면 실제 메서드 실행
```

따라서 `@Cacheable`이 붙은 메서드 안에 반드시 실행되어야 하는 부가 로직이 있으면 캐시 Hit 시 해당 로직이 실행되지 않을 수 있다.

이번 경우에는 상품 조회 결과만 캐시 대상이어야 했고, 검색 로그 저장과 인기검색어 집계는 캐시 대상이 아니어야 했다.

---

## 4. 첫 번째 해결 시도: 메서드 분리

처음에는 같은 Service 클래스 내부에서 메서드를 분리하는 방식을 고려했다.

```java
@Transactional
public ProductPageResponse searchProductsV2(...) {
    saveSearchLog(member, keyword);
    Page<ProductListItemResponse> products = searchProductsWithCache(condition, pageable);
    return ProductPageResponse.of(products);
}

@Cacheable(...)
public Page<ProductListItemResponse> searchProductsWithCache(...) {
    return productSearchRepository.search(condition, pageable);
}
```

이 방식은 책임을 나눈다는 점에서는 좋아 보였다.

```text
searchProductsV2()
→ 검색 로그 저장

searchProductsWithCache()
→ 상품 조회 캐시
```

하지만 같은 클래스 내부에서 `searchProductsWithCache()`를 호출하면 `@Cacheable`이 적용되지 않을 수 있다.

Spring AOP는 프록시 객체를 통해 호출될 때 동작하는데, 같은 클래스 내부 호출은 프록시를 거치지 않고 `this.searchProductsWithCache()`처럼 직접 호출되기 때문이다.

```text
같은 클래스 내부 호출
→ this.searchProductsWithCache()
→ Spring Proxy를 거치지 않음
→ @Cacheable 적용 안 됨
```

따라서 단순히 메서드만 분리하는 것은 안전한 해결책이 아니라고 판단했다.

---

## 5. 최종 해결 방법: 캐시 조회 전용 클래스 분리

최종적으로 검색 정책 처리와 캐시 조회 책임을 클래스로 분리했다.

```text
SearchService
→ 검색 조건 파싱
→ 검색 로그 저장
→ 인기검색어 Redis ZSet 집계
→ 응답 조립

CachedProductSearchReader
→ @Cacheable 적용
→ 상품 목록 조회
```

구조는 다음과 같다.

```text
SearchService.searchProductsV2()
→ saveSearchLog()
→ cachedProductSearchReader.search()
→ @Cacheable
→ ProductSearchRepository.search()
```

이렇게 분리하면 `SearchService`가 `CachedProductSearchReader`라는 다른 Spring Bean을 호출하게 된다.

따라서 Spring 프록시를 거쳐 `@Cacheable`이 정상적으로 동작한다.

---

## 6. 적용 코드

### SearchService

```java
@Transactional
public ProductPageResponse searchProductsV2(
        Member member,
        String keyword,
        String category,
        String status,
        String sort,
        Pageable pageable
) {
    String normalizedKeyword = normalizeKeyword(keyword);

    ProductSearchCondition condition = new ProductSearchCondition(
            normalizedKeyword,
            parseCategory(category),
            parseStatus(status),
            parseSort(sort)
    );

    saveSearchLog(member, normalizedKeyword);

    Page<ProductListItemResponse> products =
            cachedProductSearchReader.search(condition, pageable);

    return ProductPageResponse.of(products);
}
```

### CachedProductSearchReader

```java
@Cacheable(
        cacheNames = "productSearch",
        key = "@searchCacheKeyGenerator.generate(#condition, #pageable)"
)
public Page<ProductListItemResponse> search(
        ProductSearchCondition condition,
        Pageable pageable
) {
    return productSearchRepository.search(condition, pageable);
}
```

---

## 7. 적용 결과

변경 후 검색 V2의 흐름은 다음과 같이 정리되었다.

```text
검색 요청
→ SearchService.searchProductsV2()
→ 검색 로그 저장
→ 인기검색어 점수 증가
→ CachedProductSearchReader.search()
→ 캐시 확인
→ Cache Hit: Repository 조회 생략
→ Cache Miss: Repository 조회 후 캐시 저장
```

이를 통해 다음 요구사항을 모두 만족할 수 있었다.

| 요구사항 | 결과 |
| --- | --- |
| 동일 조건 반복 검색 시 DB 조회 감소 | `@Cacheable`을 통해 상품 조회 결과 캐시 |
| 검색 로그 저장 | 캐시 Hit 여부와 관계없이 SearchService에서 매번 수행 |
| 인기검색어 집계 | 캐시 Hit 여부와 관계없이 SearchService에서 매번 수행 |
| Spring Cache 정상 동작 | 별도 Bean으로 분리하여 프록시 호출 보장 |
| 책임 분리 | 검색 정책 처리와 캐시 조회 책임 분리 |

---

## 8. 테스트 및 검증

테스트에서는 Redis를 실제로 연결하지 않기 위해 `StringRedisTemplate`을 Mock 처리했다.

또한 캐시 Hit가 발생해도 검색 로그가 매번 저장되는지 확인했다.

```text
같은 조건으로 V2 검색 2회 호출
→ 상품 조회는 캐시 대상
→ 검색 로그는 2건 저장
```

검증 포인트는 다음과 같다.

| 검증 항목 | 설명 |
| --- | --- |
| 검색 로그 저장 | 같은 검색어로 2회 검색 시 SearchLog 2건 저장 |
| 캐시 적용 확인 | 같은 조건 반복 요청 시 DB 조회 로그가 1회만 출력되는지 확인 |
| Redis 테스트 분리 | 테스트 환경에서는 Redis 연결 대신 Mock 처리 |

---

## 9. 정리

이번 문제는 Caffeine 자체의 문제가 아니라 `@Cacheable`의 동작 방식과 서비스 책임 범위가 맞지 않아 발생할 수 있는 구조적 문제였다.

처음에는 검색 V2 메서드 전체에 캐시를 적용하려고 했지만, 캐시 Hit 시 메서드 실행이 생략되면서 검색 로그 저장과 인기검색어 집계까지 생략될 수 있었다.

따라서 캐시 대상은 검색 API 전체가 아니라 **상품 목록 조회 결과**로 한정해야 했다.

최종적으로 `SearchService`는 검색 정책과 기록 저장을 담당하고, `CachedProductSearchReader`는 캐시가 적용된 상품 조회만 담당하도록 분리했다.

이 과정을 통해 Spring Cache를 사용할 때는 단순히 `@Cacheable`을 붙이는 것이 아니라, **캐시 Hit 시 실행되지 않아도 되는 로직과 반드시 실행되어야 하는 로직을 분리해야 한다**는 점을 확인했다.