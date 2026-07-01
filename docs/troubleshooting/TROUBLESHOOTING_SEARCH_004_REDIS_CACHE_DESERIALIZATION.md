# [Troubleshooting] Redis 검색 캐시 조회 시 두 번째 요청에서 오류가 발생한 문제

## 1. 배경

검색 V3 API에서는 검색 결과 캐시를 Caffeine Local Cache에서 Redis Remote Cache로 확장했다.

기존 검색 V2 API는 Caffeine을 사용하여 애플리케이션 서버 내부 메모리에 검색 결과를 저장했다.

```text
검색 V2
→ Caffeine Local Cache
→ 애플리케이션 JVM 메모리에 캐시 저장
```

반면 검색 V3 API는 Redis를 사용하여 애플리케이션 외부 저장소에 검색 결과를 저장한다.

```text
검색 V3
→ Redis Remote Cache
→ Redis 서버에 캐시 저장
```

Redis를 사용하면 서버가 여러 대로 늘어나는 Scale-out 환경에서도 여러 애플리케이션 서버가 동일한 검색 캐시를 공유할 수 있다.

검색 V3의 기본 흐름은 다음과 같다.

```text
검색 요청
→ Redis 캐시 확인
→ Cache Miss
→ DB 조회
→ 조회 결과 Redis 저장
→ 응답 반환
```

이후 동일한 검색 조건으로 다시 요청하면 DB를 조회하지 않고 Redis에 저장된 검색 결과를 반환하는 것을 목표로 했다.

```text
동일 검색 요청
→ Redis 캐시 확인
→ Cache Hit
→ DB 조회 생략
→ Redis 캐시 결과 반환
```

---

## 2. 문제 상황

검색 V3 API를 테스트했을 때 첫 번째 요청은 정상적으로 동작했다.

```text
첫 번째 요청
→ Cache Miss
→ DB 조회
→ Redis에 검색 결과 저장
→ 200 OK
```

하지만 동일한 검색 조건으로 두 번째 요청을 보내면 오류가 발생했다.

```text
두 번째 요청
→ Cache Hit
→ Redis에서 캐시 조회
→ 오류 발생
→ 500 Internal Server Error
```

처음에는 Redis 연결 문제라고 생각했지만, Redis에는 실제로 검색 결과 key와 value가 저장되어 있었다.

Redis에 저장된 key 예시는 다음과 같았다.

```text
productSearch::keyword=Mac:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10
```

즉, Redis 연결이나 저장 자체는 성공했지만, 저장된 값을 다시 읽어 Java 객체로 복원하는 과정에서 문제가 발생한 것으로 판단했다.

---

## 3. 원인 분석

Redis에 저장된 value를 확인해보니 다음과 같은 형태였다.

```json
{
  "content": [
    {
      "productId": 4,
      "sellerId": 2,
      "title": "MacBook Pro 14인치",
      "price": 2500000,
      "category": "DIGITAL",
      "status": "ON_SALE",
      "thumbnailUrl": "https://cdn.example.com/images/product1.jpg",
      "createdAt": "2026-06-26T21:45:49.35862"
    }
  ],
  "empty": false,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 4,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 10,
    "paged": true,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "unpaged": false
  },
  "size": 10,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "totalElements": 4,
  "totalPages": 1
}
```

이 구조는 직접 만든 응답 DTO가 아니라 Spring Data의 `Page` 또는 `PageImpl` 구조였다.

기존 캐시 메서드는 다음과 같이 `Page<ProductListItemResponse>`를 반환하고 있었다.

```java
@Cacheable(
        cacheNames = "productSearch",
        key = "@searchCacheKeyGenerator.generate(#condition, #pageable)",
        cacheManager = "redisCacheManager"
)
public Page<ProductListItemResponse> searchWithRedis(
        ProductSearchCondition condition,
        Pageable pageable
) {
    return productSearchRepository.search(condition, pageable);
}
```

Caffeine은 JVM 메모리에 Java 객체를 그대로 저장하기 때문에 `Page` 객체를 캐싱해도 큰 문제가 없었다.

하지만 Redis는 데이터를 JSON으로 직렬화하여 저장하고, 다시 조회할 때 JSON을 Java 객체로 역직렬화해야 한다.

이때 `Page`, `PageImpl`, `Pageable`, `Sort` 같은 Spring 내부 객체는 역직렬화 과정에서 문제가 발생할 수 있다.

즉, 문제의 핵심은 다음과 같았다.

```text
Caffeine
→ Java 객체를 메모리에 그대로 저장
→ Page 객체 캐싱 가능

Redis
→ Java 객체를 JSON으로 직렬화하여 저장
→ 다시 Java 객체로 역직렬화 필요
→ PageImpl, Pageable, Sort 복원 과정에서 오류 발생 가능
```

---

## 4. 추가로 발견한 문제

V2와 V3 캐시 이름도 명확히 분리되어 있지 않았다.

기존에는 여러 캐시 설정에서 같은 이름을 사용하고 있었다.

```java
cacheNames = "productSearch"
```

하지만 V2는 Caffeine CacheManager를 사용하고, V3는 Redis CacheManager를 사용한다.

```text
V2
→ caffeineCacheManager
→ Caffeine Local Cache

V3
→ redisCacheManager
→ Redis Remote Cache
```

캐시 저장소가 다르더라도 같은 cache name을 사용하면 테스트와 디버깅 과정에서 어떤 캐시가 사용되는지 혼동될 수 있다.

또한 CaffeineCacheManager에는 기존 캐시 이름만 등록되어 있었는데, 코드에서는 변경된 캐시 이름을 사용하면서 V2 캐시도 정상적으로 동작하지 않는 문제가 발생했다.

---

## 5. 해결 방법 검토

Redis 검색 캐시 문제를 해결하기 위해 두 가지 방법을 검토했다.

| 방법                     | 설명                                           | 장점                                | 단점                                                        |
| ---------------------- | -------------------------------------------- | --------------------------------- | --------------------------------------------------------- |
| Page 객체를 그대로 Redis에 저장 | Repository 결과인 Page를 그대로 캐싱한다.               | 구현이 단순하다.                         | Redis 역직렬화 과정에서 PageImpl, Pageable, Sort 복원 문제가 발생할 수 있다. |
| 응답 DTO를 Redis에 저장      | Page를 직접 만든 ProductPageResponse로 변환한 뒤 캐싱한다. | Redis에 저장되는 구조가 단순하고 역직렬화가 안정적이다. | 캐시 메서드에서 응답 DTO 변환이 필요하다.                                 |

Redis는 외부 저장소에 JSON 형태로 데이터를 저장하므로, Spring 내부 객체보다 직접 정의한 응답 DTO를 저장하는 방식이 더 적합하다고 판단했다.

따라서 Redis 캐시 대상은 `Page<ProductListItemResponse>`가 아니라 `ProductPageResponse`로 변경했다.

---

## 6. 해결 방법 1: Redis 캐시 반환 타입 변경

기존 Redis 캐시 메서드는 `Page<ProductListItemResponse>`를 반환했다.

```java
public Page<ProductListItemResponse> searchWithRedis(
        ProductSearchCondition condition,
        Pageable pageable
) {
    return productSearchRepository.search(condition, pageable);
}
```

이를 `ProductPageResponse`를 반환하도록 변경했다.

```java
@Cacheable(
        cacheNames = "productSearchV3",
        key = "@searchCacheKeyGenerator.generate(#condition, #pageable)",
        cacheManager = "redisCacheManager"
)
public ProductPageResponse searchWithRedis(
        ProductSearchCondition condition,
        Pageable pageable
) {
    Page<ProductListItemResponse> products =
            productSearchRepository.search(condition, pageable);

    return ProductPageResponse.of(products);
}
```

이제 Redis에는 Spring Data의 `PageImpl` 구조가 아니라 직접 정의한 응답 DTO 구조가 저장된다.

정상적으로 저장되어야 하는 Redis value 형태는 다음과 같다.

```json
{
  "content": [
    {
      "productId": 4,
      "sellerId": 2,
      "title": "MacBook Pro 14인치",
      "price": 2500000,
      "category": "DIGITAL",
      "status": "ON_SALE",
      "thumbnailUrl": "https://cdn.example.com/images/product1.jpg",
      "createdAt": "2026-06-26T21:45:49.35862"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 4,
  "totalPages": 1
}
```

반대로 다음과 같은 필드가 보이면 여전히 `PageImpl`이 캐싱되고 있는 것이다.

```text
empty
first
last
number
numberOfElements
pageable
sort
```

---

## 7. 해결 방법 2: V2와 V3 캐시 이름 분리

V2와 V3는 서로 다른 CacheManager를 사용하므로 캐시 이름도 명확히 분리했다.

```text
productSearchV2
→ Caffeine Local Cache

productSearchV3
→ Redis Remote Cache
```

V2 캐시 메서드는 다음과 같이 유지했다.

```java
@Cacheable(
        cacheNames = "productSearchV2",
        key = "@searchCacheKeyGenerator.generate(#condition, #pageable)",
        cacheManager = "caffeineCacheManager"
)
public Page<ProductListItemResponse> searchWithCaffeine(
        ProductSearchCondition condition,
        Pageable pageable
) {
    return productSearchRepository.search(condition, pageable);
}
```

V2는 Caffeine을 사용하므로 `Page<ProductListItemResponse>`를 캐싱해도 된다.

Caffeine은 Redis처럼 JSON 직렬화/역직렬화를 거치지 않고 JVM 메모리에 객체를 그대로 저장하기 때문이다.

V3 캐시 메서드는 Redis에 맞게 `ProductPageResponse`를 캐싱하도록 분리했다.

```java
@Cacheable(
        cacheNames = "productSearchV3",
        key = "@searchCacheKeyGenerator.generate(#condition, #pageable)",
        cacheManager = "redisCacheManager"
)
public ProductPageResponse searchWithRedis(
        ProductSearchCondition condition,
        Pageable pageable
) {
    Page<ProductListItemResponse> products =
            productSearchRepository.search(condition, pageable);

    return ProductPageResponse.of(products);
}
```

---

## 8. 해결 방법 3: CaffeineCacheManager 캐시 이름 수정

캐시 이름을 `productSearchV2`로 변경했기 때문에 CaffeineCacheManager에도 동일한 캐시 이름을 등록해야 한다.

기존 설정은 다음과 같았다.

```java
CaffeineCacheManager cacheManager = new CaffeineCacheManager("productSearch");
```

이를 다음과 같이 수정했다.

```java
@Bean(name = "caffeineCacheManager")
@Primary
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager =
            new CaffeineCacheManager("productSearchV2");

    cacheManager.setCaffeine(
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .maximumSize(500)
    );

    return cacheManager;
}
```

이를 통해 V2 검색 API는 `productSearchV2` 캐시를 정상적으로 사용할 수 있게 되었다.

---

## 9. 해결 방법 4: Redis Serializer 변경

초기 Redis 설정에서는 범용 JSON Serializer인 `GenericJacksonJsonRedisSerializer`를 사용했다.

```java
GenericJacksonJsonRedisSerializer valueSerializer =
        new GenericJacksonJsonRedisSerializer(jsonMapper);
```

하지만 검색 V3 캐시는 `ProductPageResponse` 타입의 검색 결과 응답만 저장한다.

따라서 다양한 타입을 처리하는 Generic Serializer보다, 저장 타입을 명확히 지정하는 Serializer가 더 적합하다고 판단했다.

최종적으로 Redis value serializer는 다음과 같이 변경했다.

```java
JacksonJsonRedisSerializer<ProductPageResponse> valueSerializer =
        new JacksonJsonRedisSerializer<>(jsonMapper, ProductPageResponse.class);
```

전체 Redis CacheManager 설정은 다음과 같다.

```java
@Bean(name = "redisCacheManager")
public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    JsonMapper jsonMapper = JsonMapper.builder()
            .build();

    JacksonJsonRedisSerializer<ProductPageResponse> valueSerializer =
            new JacksonJsonRedisSerializer<>(jsonMapper, ProductPageResponse.class);

    RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues()
            .serializeKeysWith(
                    RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                    RedisSerializationContext.SerializationPair
                            .fromSerializer(valueSerializer)
            );

    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
}
```

검색 V3 캐시는 저장 타입이 고정되어 있으므로, `JacksonJsonRedisSerializer<ProductPageResponse>`를 사용해 역직렬화 안정성을 높였다.

---

## 10. 적용 결과

수정 후 동일한 검색 조건으로 V3 API를 두 번 호출해 테스트했다.

첫 번째 요청에서는 Redis에 캐시가 없으므로 DB 조회가 실행되었다.

```text
첫 번째 요청
→ Redis Cache Miss
→ DB 상품 검색 실행
→ Hibernate select/count 쿼리 실행
→ Redis에 ProductPageResponse 저장
→ 200 OK
```

두 번째 요청에서는 Redis에 저장된 검색 결과를 사용했다.

```text
두 번째 요청
→ Redis Cache Hit
→ DB 상품 검색 실행 로그 없음
→ Hibernate 쿼리 실행 없음
→ Redis 캐시 결과 반환
→ 200 OK
```

즉, Redis에 저장된 검색 결과를 정상적으로 읽어와 응답할 수 있게 되었다.

---

## 11. 최종 구조

최종 검색 캐시 구조는 다음과 같다.

```text
검색 V2
→ Caffeine Local Cache
→ cacheNames = productSearchV2
→ cacheManager = caffeineCacheManager
→ Page<ProductListItemResponse> 캐싱

검색 V3
→ Redis Remote Cache
→ cacheNames = productSearchV3
→ cacheManager = redisCacheManager
→ ProductPageResponse 캐싱
```

V2와 V3의 차이는 단순히 캐시 저장소만 다른 것이 아니라, 캐시 저장 방식도 다르다.

```text
Caffeine
→ JVM 메모리에 객체 자체를 저장
→ Page 캐싱 가능

Redis
→ JSON 직렬화 후 외부 저장소에 저장
→ Page보다 단순 DTO 캐싱이 안전
```

---

## 12. 고려한 한계

이번 해결 방식은 검색 V3 캐시 value 타입을 `ProductPageResponse`로 고정한다.

따라서 같은 RedisCacheManager를 사용해 다른 타입의 값을 캐싱하려면 별도의 CacheManager를 만들거나, 캐시별 serializer 전략을 추가로 고려해야 한다.

현재 프로젝트에서는 검색 V3 API의 Redis 캐시만 대상으로 하므로, `ProductPageResponse` 전용 serializer를 사용하는 방식이 적절하다고 판단했다.

추후 Redis 캐시 대상이 늘어난다면 다음 개선을 검토할 수 있다.

```text
캐시 이름별 RedisCacheConfiguration 분리
도메인별 RedisCacheManager 분리
Generic serializer 사용 시 타입 정보 포함 전략 적용
응답 DTO별 전용 serializer 구성
```

---

## 13. 정리

이번 문제는 Redis 검색 캐시를 적용한 뒤 첫 번째 요청은 성공하지만, 두 번째 동일 요청에서 오류가 발생한 문제였다.

원인은 Redis 연결 실패가 아니라 캐시 value의 직렬화/역직렬화 방식에 있었다.

Caffeine은 JVM 메모리에 객체를 그대로 저장하기 때문에 `Page` 객체 캐싱이 가능했지만, Redis는 JSON 직렬화와 역직렬화 과정을 거치기 때문에 `PageImpl`, `Pageable`, `Sort` 같은 Spring 내부 객체를 그대로 캐싱하면 문제가 발생할 수 있었다.

이를 해결하기 위해 Redis 검색 캐시 대상은 `Page<ProductListItemResponse>`가 아니라 `ProductPageResponse`로 변경했다.

또한 V2와 V3 캐시 이름을 `productSearchV2`, `productSearchV3`로 분리하고, V3 Redis 캐시는 `JacksonJsonRedisSerializer<ProductPageResponse>`를 사용하도록 변경했다.

이 과정을 통해 Local Cache와 Remote Cache는 저장 위치뿐만 아니라 직렬화 방식과 캐시 대상 객체 설계도 함께 고려해야 한다는 점을 확인했다.
