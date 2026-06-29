# 검색 V3 API Redis Cache 설계 결정

## 1. 검색 V3 도입 배경

검색 V2 API에서는 동일한 검색 조건의 반복 요청에 대해 DB 조회 횟수를 줄이기 위해 Caffeine Local Cache를 적용하였다.

```text
검색 요청
→ Local Cache 확인
→ Cache Hit: 캐시 응답
→ Cache Miss: DB 조회 후 캐시 저장
```

Caffeine Local Cache는 애플리케이션 서버 내부 메모리에 캐시 데이터를 저장한다.

따라서 단일 서버 환경에서는 빠르고 구현이 단순하다는 장점이 있다.

하지만 운영 환경에서 서버가 여러 대로 확장될 경우 한계가 있다.

```text
요청 1
→ 서버 A
→ 서버 A의 Local Cache에 저장

요청 2
→ 서버 B
→ 서버 B에는 해당 캐시 없음
→ 다시 DB 조회
```

즉 Local Cache는 서버 간 캐시 공유가 불가능하다.

또한 애플리케이션 서버가 재시작되면 해당 서버의 메모리에 저장된 캐시 데이터가 사라진다.

따라서 V3 검색 API에서는 Local Cache의 한계를 보완하기 위해 애플리케이션 외부에 위치한 Remote Cache 저장소를 도입한다.

---

## 2. V2 Local Cache의 한계

검색 V2에서 사용한 Caffeine Local Cache의 한계는 다음과 같다.

| 한계                 | 설명                                          |
| ------------------ | ------------------------------------------- |
| 서버 간 캐시 공유 불가      | 서버가 여러 대일 경우 각 서버가 독립적인 캐시를 가진다.            |
| 애플리케이션 재시작 시 캐시 소멸 | 캐시가 JVM 메모리에 저장되므로 서버 재시작 시 캐시가 사라진다.       |
| Scale-out 환경에 부적합  | 로드밸런서를 통해 다른 서버로 요청이 전달되면 기존 캐시를 재사용할 수 없다. |
| 캐시 관리 분산           | 서버별로 캐시 상태가 달라질 수 있어 일관된 캐시 관리가 어렵다.        |

정리하면 Caffeine Local Cache는 단일 서버 기준에서는 적합하지만, 서버 확장성과 캐시 공유를 고려하면 한계가 있다.

따라서 V3에서는 여러 서버가 동일한 캐시 데이터를 공유할 수 있는 Remote Cache가 필요하다.

---

## 3. Remote Cache 저장소 후보 비교

Remote Cache 저장소로 다음 후보들을 검토하였다.

| 후보           | 장점                                                | 단점                                    | 프로젝트 적용 판단                                        |
| ------------ | ------------------------------------------------- | ------------------------------------- | ------------------------------------------------- |
| Redis        | 빠른 Key-Value 조회가 가능하다. TTL 설정이 쉽고 다양한 자료구조를 제공한다. | 별도 Redis 서버 운영이 필요하다. 네트워크 통신 비용이 있다. | 검색 결과 캐시, 인기 검색어, 최근 검색어, 토큰 관리 등으로 확장 가능하여 적합하다. |
| Memcached    | 단순 Key-Value 캐시에 특화되어 있고 구조가 단순하다.                | 자료구조가 제한적이다. 순위 기반 기능 구현이 어렵다.        | 검색 결과 캐시만 보면 가능하지만 인기 검색어까지 고려하면 Redis가 더 적합하다.   |
| Hazelcast    | 분산 캐시와 인메모리 데이터 그리드를 지원한다.                        | 설정과 운영 복잡도가 높다.                       | 현재 팀 프로젝트 규모에서는 과한 선택이라고 판단했다.                    |
| DB 기반 캐시 테이블 | 별도 캐시 서버 없이 기존 DB만으로 구현할 수 있다.                    | 캐시 조회도 DB에서 발생하므로 DB 부하 감소 효과가 제한적이다. | 캐시의 목적이 DB 부하 감소이므로 적합하지 않다.                      |

---

## 4. Redis 선택

이번 검색 V3 API에서는 Remote Cache 저장소로 Redis를 선택한다.

선택 이유는 다음과 같다.

| 선택 이유         | 설명                                                             |
| ------------- | -------------------------------------------------------------- |
| Scale-out 적합성 | 여러 애플리케이션 서버가 동일한 Redis 캐시를 공유할 수 있다.                          |
| 빠른 조회 성능      | 인메모리 기반 Key-Value 저장소이므로 반복 조회에 적합하다.                          |
| TTL 지원        | 검색 결과 캐시의 만료 시간을 쉽게 설정할 수 있다.                                  |
| 다양한 자료구조      | String, List, Set, Sorted Set, Hash 등을 제공한다.                   |
| 기능 확장성        | 검색 결과 캐시뿐 아니라 인기 검색어, 최근 검색어, 로그아웃 토큰 관리 등에 활용할 수 있다.          |
| Spring 연동성    | Spring Cache, RedisTemplate, StringRedisTemplate과 쉽게 연동할 수 있다. |

Redis를 사용하면 다음과 같은 구조가 된다.

```text
서버 A ┐
서버 B ├── Redis Cache
서버 C ┘
```

따라서 어떤 애플리케이션 서버로 요청이 들어오더라도 동일한 캐시 데이터를 조회할 수 있다.

정리하면 Redis는 단순 검색 결과 캐시뿐 아니라 프로젝트 내 다른 기능으로도 확장 가능하고, Scale-out 환경에서 서버 간 캐시 공유가 가능하므로 V3 Remote Cache 저장소로 적합하다고 판단했다.

---

## 5. Redis 자료구조 선택

Redis는 다양한 자료구조를 제공한다.

검색 V3에서는 기능의 목적에 따라 Redis 자료구조를 구분하여 사용한다.

| 자료구조       | 특징                                          | 검색 기능 적용 판단                    |
| ---------- | ------------------------------------------- | ------------------------------ |
| String     | 하나의 key에 하나의 value를 저장하는 기본 Key-Value 구조이다. | 검색 결과 캐시에 적합하다.                |
| List       | 입력 순서를 유지하는 목록 구조이다.                        | 최근 검색어처럼 시간순 목록이 필요한 경우에 적합하다. |
| Set        | 중복을 허용하지 않는 집합 구조이다.                        | 중복 제거가 중요한 데이터에 적합하다.          |
| Sorted Set | 각 데이터에 score를 부여하고 score 기준으로 정렬할 수 있다.     | 인기 검색어 TOP 10에 적합하다.           |
| Hash       | 하나의 key 안에 여러 field-value를 저장한다.            | 객체의 일부 필드만 조회하거나 수정할 때 적합하다.   |

---

## 6. 검색 결과 캐시에 String을 선택한 이유

검색 결과 캐시는 다음과 같은 구조로 저장된다.

```text
key   = 검색 조건을 조합한 문자열
value = 검색 결과 응답 JSON
```

예를 들어 다음 요청이 있다고 가정한다.

```text
GET /api/v3/products/search?keyword=맥북&category=DIGITAL&status=ON_SALE&sort=LATEST&page=0&size=10
```

이 요청에 대한 캐시 데이터는 다음과 같이 저장할 수 있다.

```text
key   = productSearchV3::keyword=맥북:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10
value = ProductPageResponse JSON
```

검색 결과 캐시의 목적은 Redis 내부에서 개별 상품을 다시 정렬하거나, 특정 필드만 수정하는 것이 아니다.

DB와 QueryDSL에서 이미 정렬과 페이징이 완료된 검색 결과 응답을 동일한 검색 조건의 요청이 들어왔을 때 그대로 재사용하는 것이 목적이다.

따라서 검색 조건을 key로, 검색 결과 응답 전체를 value로 저장하는 Redis String 자료구조가 가장 적합하다.

---

## 7. 인기 검색어에 Sorted Set을 선택한 이유

인기 검색어 기능은 검색어별 검색 횟수를 누적하고, 검색 횟수가 높은 순서대로 TOP 10을 조회해야 한다.

예를 들어 다음과 같은 데이터가 필요하다.

```text
맥북   120회
아이폰 95회
에어팟 60회
```

이 구조는 단순 Key-Value 저장보다 score 기반 정렬이 필요하다.

Redis Sorted Set은 member와 score를 함께 저장할 수 있다.

```text
member = 검색어
score  = 검색 횟수
```

검색어가 입력될 때마다 score를 증가시키고, 조회 시 score가 높은 순서대로 가져오면 인기 검색어 TOP 10을 쉽게 구현할 수 있다.

```text
ZINCRBY popular:keywords 1 "맥북"
ZINCRBY popular:keywords 1 "아이폰"

ZREVRANGE popular:keywords 0 9 WITHSCORES
```

따라서 검색 결과 캐시는 String, 인기 검색어는 Sorted Set을 사용하는 방식으로 기능 목적에 맞게 자료구조를 분리한다.

---

## 8. Redis Serializer 선택

Redis는 Java 객체를 그대로 저장하는 것이 아니라 문자열 또는 byte 형태로 데이터를 저장한다.

따라서 Redis에 저장할 key와 value를 어떤 방식으로 직렬화할지 결정해야 한다.

검색 결과 캐시는 다음과 같은 구조로 저장된다.

```text
key   = 검색 조건을 조합한 문자열
value = 검색 결과 응답 DTO
```

### Serializer 후보 비교

| Serializer                         | 장점                                              | 단점                                                                      | 적용 판단                  |
| ---------------------------------- | ----------------------------------------------- | ----------------------------------------------------------------------- | ---------------------- |
| StringRedisSerializer              | 문자열로 저장되어 사람이 읽기 쉽다.                            | 복잡한 객체 저장에는 적합하지 않다.                                                    | key 저장에 사용한다.          |
| GenericJacksonJsonRedisSerializer  | 다양한 Java 객체를 JSON으로 저장할 수 있다.                   | 역직렬화 시 타입 정보 처리가 필요하고, 특정 응답 타입만 캐싱하는 경우에는 과할 수 있다.                     | 검토했지만 최종 선택하지 않았다.     |
| JacksonJsonRedisSerializer<T>      | 특정 타입을 명확히 지정하여 JSON으로 저장하고 읽을 수 있다.            | 타입을 명확히 지정해야 하므로 여러 타입을 하나의 CacheManager에서 다루기에는 유연성이 떨어진다.             | 검색 V3 응답 DTO 저장에 사용한다. |
| Jackson2JsonRedisSerializer<T>     | 기존 Jackson 2 환경에서 특정 타입을 JSON으로 저장할 수 있다.       | Spring Data Redis 4.x / Jackson 3 환경에서는 deprecated 흐름이므로 장기적으로 적합하지 않다. | 제외한다.                  |
| GenericJackson2JsonRedisSerializer | 기존 Jackson 2 환경에서 다양한 Java 객체를 JSON으로 저장할 수 있다. | Spring Data Redis 4.x / Jackson 3 환경에서는 적합하지 않다.                        | 제외한다.                  |
| JdkSerializationRedisSerializer    | Java 객체 직렬화가 가능하다.                              | 사람이 읽기 어렵고 Java 의존성이 강하다.                                               | 제외한다.                  |
| GenericToStringSerializer          | 숫자, 문자열 같은 단순 값 저장에 적합하다.                       | 복잡한 DTO 저장에는 부적합하다.                                                     | 검색 결과 캐시에는 부적합하다.      |

### Key Serializer 선택

Redis key는 검색 조건을 조합한 문자열이다.

예를 들어 다음과 같은 key를 사용한다.

```text
productSearchV3::keyword=맥북:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10
```

key는 Redis CLI나 RedisInsight에서 확인했을 때 사람이 읽을 수 있어야 운영과 디버깅이 쉽다.

따라서 key에는 `StringRedisSerializer`를 사용한다.

### Value Serializer 선택

Redis value에는 검색 결과 응답 DTO를 저장해야 한다.

검색 결과 응답은 상품 목록, 페이지 정보, 전체 개수 등을 포함하는 객체이다.

```text
ProductPageResponse
 ├── content
 ├── page
 ├── size
 ├── totalElements
 └── totalPages
```

초기에는 다양한 응답 객체를 캐싱할 가능성을 고려하여 `GenericJacksonJsonRedisSerializer`를 검토했다.

하지만 검색 V3 캐시는 `ProductPageResponse` 타입의 검색 결과 응답만 저장한다.

따라서 범용 Serializer보다 타입을 명확히 지정할 수 있는 `JacksonJsonRedisSerializer<ProductPageResponse>`가 더 적합하다고 판단했다.

실제로 Redis에 `Page<ProductListItemResponse>` 또는 `PageImpl` 구조가 저장되었을 때, 첫 번째 요청에서는 Redis 저장이 성공했지만 두 번째 요청에서 Redis value를 다시 Java 객체로 역직렬화하는 과정에서 오류가 발생했다.

`PageImpl`, `Pageable`, `Sort` 같은 Spring 내부 객체는 Redis JSON 역직렬화 대상으로 적합하지 않다.

따라서 Redis 캐시에는 Spring 내부 객체인 `Page`가 아니라, 직접 정의한 응답 DTO인 `ProductPageResponse`를 저장하도록 변경했다.

최종적으로 Redis value serializer는 다음과 같이 결정한다.

```java
JacksonJsonRedisSerializer<ProductPageResponse> valueSerializer =
        new JacksonJsonRedisSerializer<>(jsonMapper, ProductPageResponse.class);
```

정리하면 Redis key에는 `StringRedisSerializer`, value에는 `JacksonJsonRedisSerializer<ProductPageResponse>`를 사용한다.

---

## 9. 캐시 전략 선택

검색 V3에서도 V2와 동일하게 Cache-aside 전략을 사용한다.

검색 API는 데이터를 저장하거나 수정하는 기능이 아니라 읽기 중심 기능이다.

따라서 캐시에 같은 검색 조건의 결과가 있으면 캐시에서 반환하고, 없으면 DB를 조회한 뒤 Redis에 저장하는 방식이 적합하다.

```text
요청
→ Redis Cache 확인
→ Cache Hit: Redis 결과 반환
→ Cache Miss: DB 조회
→ DB 조회 결과를 Redis에 저장
→ 응답
```

Spring의 `@Cacheable`은 기본적으로 Cache-aside 방식으로 동작하므로 기존 V2 구조를 크게 변경하지 않고 캐시 저장소만 Redis로 확장할 수 있다.

---

## 10. 캐시 key 설계

검색 결과 캐시의 key에는 검색 결과에 영향을 주는 모든 조건이 포함되어야 한다.

검색 결과에 영향을 주는 값은 다음과 같다.

| 값             | key에 포함해야 하는 이유                      |
| ------------- | ------------------------------------ |
| keyword       | 검색어가 다르면 결과가 달라진다.                   |
| category      | 카테고리가 다르면 결과가 달라진다.                  |
| status        | 판매 상태 필터가 다르면 결과가 달라진다.              |
| sortType      | 검색 정렬 조건이 다르면 결과 순서가 달라진다.           |
| page          | 페이지 번호가 다르면 반환되는 상품 목록이 달라진다.        |
| size          | 페이지 크기가 다르면 반환되는 상품 개수가 달라진다.        |
| pageable sort | Pageable 정렬 정보가 다르면 결과 순서가 달라질 수 있다. |

예를 들어 아래 요청은 같은 keyword를 사용하지만 서로 다른 결과이다.

```text
keyword=맥북&page=0&size=10
keyword=맥북&page=1&size=10
```

또 아래 요청은 같은 keyword와 page를 사용하지만 정렬 조건이 다르다.

```text
keyword=맥북&sort=LATEST&page=0&size=10
keyword=맥북&sort=PRICE_ASC&page=0&size=10
```

따라서 key는 다음과 같은 형태로 설계한다.

```text
productSearchV3::keyword={keyword}:category={category}:status={status}:sortType={sortType}:page={page}:size={size}:sort={pageableSort}
```

실제 구현에서는 V2에서 사용한 `SearchCacheKeyGenerator`를 재사용한다.

다만 V2와 V3는 서로 다른 CacheManager와 cache name을 사용한다.

```text
V2
→ cacheNames = productSearchV2
→ cacheManager = caffeineCacheManager

V3
→ cacheNames = productSearchV3
→ cacheManager = redisCacheManager
```

V3 Redis 검색 캐시는 다음과 같이 적용한다.

```java
@Cacheable(
        cacheNames = "productSearchV3",
        key = "@searchCacheKeyGenerator.generate(#condition, #pageable)",
        cacheManager = "redisCacheManager"
)
```

---

## 11. 검색어 기록과 캐시 분리

검색 V3에서도 검색어 기록과 상품 검색 결과 캐시는 분리한다.

검색 정책은 다음과 같다.

```text
상품 검색
→ 로그인/비로그인 모두 가능

검색어 기록 저장
→ 로그인 사용자의 keyword 검색만 저장

인기검색어 집계
→ 로그인 사용자의 keyword 검색만 Redis Sorted Set에 반영
```

`@Cacheable`이 붙은 메서드 내부에서 검색어 기록을 저장하면 캐시 hit 시 메서드 자체가 실행되지 않는다.

```text
Cache Hit
→ @Cacheable 메서드 실행 안 됨
→ 검색어 기록 저장 안 됨
→ 인기검색어 집계 안 됨
```

하지만 사용자가 검색 API를 호출했다면 캐시 hit 여부와 상관없이 검색 행위는 기록되어야 한다.

따라서 검색어 기록 저장과 인기검색어 집계는 캐시가 적용된 조회 메서드 밖에서 처리한다.

```text
SearchService
→ 검색 정책 처리
→ 검색어 기록 저장
→ 인기검색어 Redis Sorted Set 집계
→ CachedProductSearchReader 호출

CachedProductSearchReader
→ @Cacheable 적용
→ 상품 검색 결과 조회
```

이 구조를 사용하면 검색어 기록은 캐시 hit 여부와 상관없이 수행되고, 상품 검색 결과 조회만 캐시의 영향을 받는다.

---

## 12. 캐시 갱신 방식 선택: CachePut vs CacheEvict

검색 결과 캐시는 상품 데이터에 영향을 받는다.

따라서 상품 등록, 수정, 삭제, 상태 변경, 이미지 변경 등이 발생하면 기존 검색 결과 캐시는 오래된 데이터를 포함할 수 있다.

상품 변경 이후 캐시를 최신 상태로 만드는 방식은 크게 두 가지를 검토할 수 있다.

| 방식 | 설명 | 장점 | 단점 | 적용 판단 |
| --- | --- | --- | --- | --- |
| `@CachePut` | 메서드를 항상 실행하고, 실행 결과를 캐시에 저장하거나 갱신한다. | 특정 key의 캐시를 즉시 최신 값으로 갱신할 수 있다. | 갱신해야 할 cache key를 정확히 알아야 한다. | 검색 결과 캐시에는 부적합하다. |
| `@CacheEvict` | 기존 캐시를 삭제한다. | 영향을 받는 캐시를 단순하게 제거할 수 있다. | 다음 조회 시 DB를 다시 조회해야 한다. | 검색 결과 캐시에 적합하다. |

`@CachePut`은 캐시를 먼저 조회해서 반환하는 방식이 아니다.

메서드는 항상 실행되고, 그 실행 결과가 캐시에 저장된다.

```text
요청
→ 메서드 항상 실행
→ DB 조회 또는 DB 수정
→ 메서드 반환값 생성
→ 반환값을 캐시에 저장 또는 갱신
```

따라서 `@CachePut`은 갱신 대상 cache key가 명확한 경우에 적합하다.

예를 들어 상품 상세 캐시는 상품 ID를 기준으로 key가 고정된다.

```text
productDetail::1
```

상품 1번을 수정했다면 갱신해야 할 캐시 key도 명확하다.

```text
상품 1번 수정
→ DB 수정
→ ProductDetailResponse 생성
→ productDetail::1 캐시 갱신
```

하지만 검색 결과 캐시는 key가 검색 조건 조합별로 생성된다.

```text
productSearchV3::keyword=Mac:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10
productSearchV3::keyword=MacBook:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10
productSearchV3::keyword=맥북:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10
productSearchV3::keyword=null:category=DIGITAL:status=ON_SALE:sortType=PRICE_ASC:page=0:size=10
```

상품 하나는 여러 검색 조건의 결과에 포함될 수 있다.

상품을 수정했을 때 `@CachePut`으로 캐시를 갱신하려면 다음 작업이 필요하다.

```text
1. 수정된 상품이 포함된 모든 검색 캐시 key 추적
2. 각 검색 조건별 최신 검색 결과 재조회
3. 각 key에 최신 검색 결과 다시 저장
```

현재 프로젝트에서는 검색 캐시 key와 상품 ID의 매핑 정보를 별도로 관리하지 않는다.

또한 사용자가 어떤 keyword, category, status, sort, page 조합으로 검색했는지 모든 캐시 key를 추적하지 않는다.

따라서 검색 결과 캐시는 특정 key를 갱신하는 `@CachePut`보다, 영향을 받을 수 있는 검색 캐시를 삭제하는 `@CacheEvict` 방식이 더 적합하다고 판단했다.

---

## 13. 즉시 무효화와 자연 만료 선택 기준

검색 캐시를 오래된 상태로 두지 않기 위한 방법은 크게 두 가지가 있다.

| 방식 | 설명 | 장점 | 단점 | 적용 판단 |
| --- | --- | --- | --- | --- |
| 자연 만료 | TTL이 끝날 때까지 기다린 뒤 캐시가 자동 만료된다. | 구현이 단순하고 캐시 효율이 좋다. | 상품 변경 직후에는 오래된 데이터가 노출될 수 있다. | 단독 사용은 부적합하다. |
| 즉시 무효화 | 상품 변경 시점에 관련 캐시를 삭제한다. | 변경 이후 다음 검색부터 최신 DB 기준 결과를 반환할 수 있다. | 캐시가 비워져 일시적으로 DB 조회가 증가한다. | 적용한다. |

TTL은 오래된 캐시를 eventually 제거할 수 있다.

하지만 상품 변경 직후의 최신성은 보장하지 못한다.

예를 들어 상품 A가 검색 결과에 캐시된 상태에서 판매 상태가 변경될 수 있다.

```text
상품 A 검색 결과 캐시 저장
→ 상품 A 상태 변경
→ ON_SALE → SOLD
→ TTL이 아직 남아 있음
→ 동일 검색 요청 시 Cache Hit
→ 이전 검색 결과 반환 가능
```

검색 결과는 결제나 재고 차감처럼 강한 정합성이 필요한 데이터는 아니다.

하지만 상품 상태, 삭제 여부, 가격 변경은 사용자 화면에 직접 노출되는 정보이다.

예를 들어 이미 판매완료된 상품이 계속 판매중으로 보이면 사용자가 잘못된 상품에 대해 채팅을 시도할 수 있다.

```text
판매중으로 보고 채팅 요청
→ 실제로는 판매완료
→ 사용자 혼란 발생
```

따라서 이번 프로젝트에서는 TTL 자연 만료만 기다리지 않고, 상품 변경 시 검색 캐시를 즉시 무효화한다.

```text
상품 등록/수정/삭제/상태 변경
→ productSearchV2 캐시 삭제
→ productSearchV3 캐시 삭제
→ 다음 검색 요청
→ Cache Miss
→ DB 조회
→ 최신 검색 결과 캐시 저장
```

정리하면 자연 만료는 기본 안전장치로 사용하고, 상품 변경 이벤트가 발생한 경우에는 즉시 무효화를 적용한다.

---

## 14. TTL 설정 결정

검색 V2와 V3의 검색 결과 캐시 TTL은 5분으로 설정한다.

```text
TTL
→ 5분
```

TTL은 캐시 효율과 데이터 신선성 사이의 균형을 기준으로 결정했다.

검색 결과 캐시는 다음과 같은 상품 데이터 변경에 영향을 받는다.

```text
상품 등록
상품 수정
상품 삭제
상품 상태 변경
가격 변경
판매완료 처리
상품 이미지 변경
```

TTL이 너무 길면 캐시 효율은 좋아지지만 오래된 상품 정보가 사용자에게 노출될 가능성이 커진다.

```text
TTL 30분
→ DB 조회 감소 효과는 큼
→ 상품 상태 변경, 가격 변경, 삭제 반영이 늦어질 수 있음
```

반대로 TTL이 너무 짧으면 데이터 신선성은 좋아지지만 캐시 효과가 약해진다.

```text
TTL 10초
→ 최신성은 좋음
→ 동일 검색어 반복 요청에서도 캐시가 금방 만료됨
→ DB 조회 감소 효과가 작음
```

이번 프로젝트에서는 검색 결과가 매우 강한 정합성을 요구하는 데이터는 아니지만, 상품 상태와 삭제 여부가 너무 오래 틀리게 보이면 사용자 경험에 문제가 생길 수 있다고 판단했다.

따라서 TTL 후보를 다음과 같이 비교했다.

| TTL 후보 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- |
| 30초 ~ 1분 | 최신성이 높다. | 반복 검색 캐시 효과가 낮다. | 캐시 성능 개선 목적이 약해진다. |
| 5분 | 반복 검색에 대한 캐시 효과를 기대할 수 있고, 오래된 데이터 노출 시간도 비교적 짧다. | 상품 변경 직후에는 최대 5분간 오래된 캐시가 남을 수 있다. | 즉시 무효화와 함께 사용하면 적절하다. |
| 30분 이상 | 캐시 hit 가능성이 높다. | 상품 상태, 가격, 삭제 여부 반영이 늦어질 수 있다. | 검색 결과 최신성 측면에서 부적합하다. |

최종적으로 TTL은 5분으로 결정했다.

단, TTL만으로 최신성을 보장하지 않는다.

상품 변경이 발생하면 `@CacheEvict`로 검색 캐시를 즉시 삭제하고, TTL은 캐시가 비정상적으로 오래 남지 않도록 하는 보조 안전장치로 사용한다.

```text
기본 정책
→ TTL 5분으로 자연 만료

상품 변경 발생
→ TTL을 기다리지 않고 즉시 무효화
```

이 조합을 통해 반복 검색에 대한 캐시 효과와 상품 데이터 최신성의 균형을 맞춘다.

---

## 15. Eviction Policy 설계

이번 프로젝트의 검색 캐시 Eviction Policy는 다음 기준으로 설계한다.

```text
1. 시간 기반 만료
2. 크기 기반 제한
3. 데이터 변경 기반 즉시 무효화
4. Redis 서버 메모리 정책
```

검색 캐시는 원본 데이터가 아니라 반복 조회 성능을 높이기 위한 보조 데이터이다.

따라서 메모리가 부족한 상황에서는 영구 보존보다 오래된 캐시를 제거하고, 원본 DB를 다시 조회해 캐시를 재생성하는 방식이 더 적합하다.

### 15-1. 시간 기반 만료

검색 결과 캐시는 V2와 V3 모두 TTL 5분을 적용한다.

```text
V2 Caffeine
→ expireAfterWrite(Duration.ofMinutes(5))

V3 Redis
→ entryTtl(Duration.ofMinutes(5))
```

시간 기반 만료는 캐시가 영구적으로 남지 않도록 하는 기본 정책이다.

캐시 무효화 로직이 누락되거나 특정 상황에서 실행되지 않더라도, TTL이 지나면 오래된 캐시는 자동으로 제거된다.

### 15-2. 크기 기반 제한

V2 Caffeine Local Cache는 애플리케이션 JVM 메모리를 사용한다.

따라서 캐시가 무제한 증가하면 애플리케이션 메모리 사용량이 계속 증가할 수 있다.

이를 방지하기 위해 최대 캐시 개수를 500개로 제한한다.

```java
Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .maximumSize(500)
```

`maximumSize(500)`은 로컬 메모리에 저장되는 검색 결과 캐시의 최대 개수를 제한한다.

캐시 크기가 제한을 초과하면 Caffeine 내부 정책에 따라 상대적으로 덜 유용한 항목이 제거된다.

현재 프로젝트는 학습용 팀 프로젝트이므로 초기 기준값으로 500개를 선택했다.

이 값은 운영 환경의 트래픽, 검색 조건 다양성, 메모리 사용량을 측정한 뒤 조정할 수 있다.

### 15-3. 데이터 변경 기반 즉시 무효화

상품 데이터가 변경되면 검색 결과 캐시는 오래된 데이터를 포함할 수 있다.

따라서 상품 등록, 수정, 삭제, 상태 변경, 이미지 변경 시 검색 캐시를 즉시 무효화한다.

```java
@Caching(evict = {
        @CacheEvict(
                cacheNames = "productSearchV2",
                allEntries = true,
                cacheManager = "caffeineCacheManager"
        ),
        @CacheEvict(
                cacheNames = "productSearchV3",
                allEntries = true,
                cacheManager = "redisCacheManager"
        )
})
```

V2와 V3는 서로 다른 CacheManager를 사용하므로 두 캐시를 모두 삭제한다.

```text
productSearchV2
→ Caffeine Local Cache

productSearchV3
→ Redis Remote Cache
```

검색 캐시는 검색 조건 조합별로 key가 생성된다.

상품 하나가 어떤 검색 조건의 캐시에 포함되어 있는지 정확히 알기 어렵기 때문에 특정 key만 삭제하지 않고 `allEntries = true`로 전체 검색 캐시를 무효화한다.

### 15-4. Redis 서버 Eviction Policy

Redis는 서버 메모리가 부족할 때 어떤 key를 제거할지 결정하는 `maxmemory-policy`를 설정할 수 있다.

대표적인 정책은 다음과 같다.

| 정책 | 설명 | 적용 판단 |
| --- | --- | --- |
| noeviction | 메모리 한도 초과 시 새 쓰기 요청을 거부한다. | 캐시 저장 실패 가능성이 있으므로 캐시 전용 Redis에는 부적합할 수 있다. |
| allkeys-lru | 모든 key 중 최근에 덜 사용된 key를 제거한다. | 캐시 전용 Redis라면 검토 가능하다. |
| allkeys-lfu | 모든 key 중 사용 빈도가 낮은 key를 제거한다. | 반복 조회가 적은 캐시를 제거하는 데 적합하다. |
| volatile-lru | TTL이 설정된 key 중 최근에 덜 사용된 key를 제거한다. | TTL key 중심으로 관리할 때 검토 가능하다. |
| volatile-ttl | TTL이 설정된 key 중 만료 시간이 가까운 key를 먼저 제거한다. | 검색 캐시만 보면 적합하지만, 현재 Redis 전체에 적용하기에는 보류한다. |

검색 결과 캐시만 고려하면 `volatile-ttl` 정책이 적합할 수 있다.

검색 결과 캐시는 TTL 5분이 설정된 임시 데이터이고, 만료 시간이 가까운 캐시는 곧 자연 만료될 데이터이기 때문이다.

```text
검색 결과 캐시
→ TTL 5분
→ 만료 시간이 가까운 캐시부터 제거해도 영향이 비교적 작음
→ 제거되더라도 다음 요청에서 DB 조회 후 재생성 가능
```

하지만 Redis의 `maxmemory-policy`는 특정 cacheName에만 적용되는 설정이 아니라 Redis 인스턴스 전체에 적용되는 서버 설정이다.

현재 프로젝트에서는 Redis를 검색 캐시뿐 아니라 인증 관련 데이터에도 사용할 수 있다.

예를 들어 다음과 같은 데이터도 Redis에 저장될 수 있다.

```text
검색 결과 캐시
→ TTL 있음

Refresh Token
→ TTL 있음

Logout Token / Access Token Blacklist
→ TTL 있음

인기 검색어 Sorted Set
→ TTL 없을 수 있음
```

`volatile-ttl`을 적용하면 TTL이 설정된 key 중 만료 시간이 가까운 key가 제거 대상이 된다.

이 경우 검색 결과 캐시뿐 아니라 인증 관련 key도 제거 후보가 될 수 있다.

인증 관련 key가 Redis 메모리 부족으로 조기 제거되면 다음과 같은 문제가 발생할 수 있다.

```text
Refresh Token이 조기 제거됨
→ 사용자는 토큰 만료 전이라고 생각하지만 재발급 실패 가능
→ 의도하지 않은 로그아웃 발생 가능

Logout Token 또는 Access Token Blacklist가 조기 제거됨
→ 로그아웃 처리된 토큰이 만료 전까지 다시 사용될 가능성
→ 보안 정책상 위험
```

검색 결과 캐시는 제거되더라도 DB에서 다시 조회해 재생성하면 된다.

반면 인증 관련 데이터는 임의 제거될 경우 사용자 인증 흐름이나 보안 정책에 영향을 줄 수 있다.

따라서 현재 프로젝트에서는 Redis 서버의 `maxmemory-policy`를 별도로 설정하지 않는다.

```text
현재 결정
→ Redis maxmemory-policy 미설정
```

검색 캐시는 애플리케이션 레벨에서 다음 정책으로 관리한다.

```text
1. Redis Cache TTL 5분
2. 상품 변경 시 @CacheEvict 즉시 무효화
3. Caffeine Local Cache maximumSize 500
```

추후 운영 환경에서 검색 캐시용 Redis와 인증용 Redis를 분리할 경우, 검색 캐시 전용 Redis에는 `volatile-ttl` 정책 적용을 검토한다.

```text
redis-cache
→ 검색 결과 캐시
→ volatile-ttl 적용 검토

redis-auth
→ Refresh Token, Logout Token
→ 인증 안정성을 우선하는 별도 정책 적용
```

정리하면 `volatile-ttl`은 검색 캐시만 놓고 보면 적합한 정책이지만, 현재 프로젝트처럼 Redis를 여러 용도로 함께 사용할 수 있는 구조에서는 전체 Redis에 적용하지 않는 것이 더 안전하다고 판단했다.


---

## 16. 구현 반영 내용

구현에서는 다음 파일 또는 설정을 기준으로 Redis Cache를 적용한다.

```text
RedisConfig
→ RedisCacheManager 설정
→ key serializer: StringRedisSerializer
→ value serializer: JacksonJsonRedisSerializer<ProductPageResponse>
→ cacheName: productSearchV3
→ TTL: 5분

CacheConfig
→ CaffeineCacheManager 설정
→ cacheName: productSearchV2
→ TTL: 5분
→ maximumSize: 500

SearchCacheKeyGenerator
→ keyword, category, status, sortType, page, size, pageable sort 기반 key 생성

SearchService
→ 검색 조건 파싱
→ 검색 로그 저장
→ 인기검색어 Redis Sorted Set 집계
→ CachedProductSearchReader 호출

CachedProductSearchReader
→ V2: Caffeine Cache 적용
→ V3: Redis Cache 적용
→ 상품 검색 결과 조회

ProductCommandService
→ 상품 등록, 수정, 삭제, 상태 변경 시 검색 캐시 무효화
```

V3 검색 흐름은 다음과 같다.

```text
/api/v3/products/search 요청
→ SearchFacade
→ SearchService.searchProductsV3()
→ 검색 조건 파싱
→ 검색 로그 저장
→ 인기검색어 Redis Sorted Set 집계
→ CachedProductSearchReader.searchWithRedis()
→ Redis Cache Hit 여부 확인
→ Hit: Redis 캐시 결과 반환
→ Miss: DB 조회 후 Redis 저장
→ ProductPageResponse 응답
```

V2와 V3의 캐시 적용 메서드는 다음과 같이 분리한다.

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

V2는 Caffeine Local Cache를 사용하므로 JVM 메모리에 `Page<ProductListItemResponse>` 객체를 그대로 저장할 수 있다.

반면 V3는 Redis Remote Cache를 사용하므로 JSON 직렬화와 역직렬화를 고려해 `ProductPageResponse`를 저장한다.

---

## 17. 최종 설계 결정

이번 검색 V3 Redis Cache 설계는 다음과 같이 결정한다.

| 항목 | 결정 |
| --- | --- |
| 캐시 적용 API | `/api/v3/products/search` |
| 기존 API 유지 | `/api/v1/products/search`, `/api/v2/products/search` 유지 |
| V2 캐시 저장소 | Caffeine Local Cache |
| V3 캐시 저장소 | Redis Remote Cache |
| 캐시 전략 | Cache-aside |
| 캐시 갱신 방식 | `@CachePut`이 아니라 `@CacheEvict` 선택 |
| 캐시 갱신 방식 선택 이유 | 검색 캐시는 key 조합이 많고 상품이 어떤 검색 key에 포함되는지 추적하기 어렵기 때문 |
| V2 캐시 이름 | `productSearchV2` |
| V3 캐시 이름 | `productSearchV3` |
| 검색 결과 캐시 자료구조 | Redis String |
| 인기검색어 자료구조 | Redis Sorted Set |
| TTL | 5분 |
| TTL 선택 이유 | 반복 검색 캐시 효과와 상품 데이터 신선성의 균형 |
| Eviction Policy | TTL 기반 자연 만료 + 상품 변경 시 즉시 무효화 + Caffeine maximumSize 제한 |
| Redis 서버 maxmemory-policy | 현재 미설정, Redis 분리 후 검색 캐시 전용 Redis에 `volatile-ttl` 적용 검토 |
| 캐시 key | keyword + category + status + sortType + page + size + pageable sort |
| 캐시 key 생성 | `SearchCacheKeyGenerator` 재사용 |
| Key Serializer | StringRedisSerializer |
| Value Serializer | JacksonJsonRedisSerializer<ProductPageResponse> |
| V2 캐시 대상 | Page<ProductListItemResponse> |
| V3 캐시 대상 | ProductPageResponse |
| 검색어 기록 저장 | 캐시와 분리하여 항상 처리 |
| 인기검색어 집계 | 캐시와 분리하여 항상 처리 |
| 캐시 적용 클래스 | `CachedProductSearchReader` |
| 캐시 무효화 | 상품 변경 시 productSearchV2, productSearchV3 전체 삭제 |
| 개선 목적 | Local Cache의 서버 간 공유 불가 문제 보완 |

정리하면 검색 V3 API에서는 Caffeine Local Cache의 한계인 서버 간 캐시 공유 불가 문제를 보완하기 위해 Redis Remote Cache를 도입한다.

Redis는 여러 애플리케이션 서버가 동일한 캐시 데이터를 공유할 수 있어 Scale-out 환경에 적합하다.

검색 결과 캐시는 이미 정렬과 페이징이 완료된 응답 결과를 동일 조건 요청 시 그대로 재사용하는 목적이므로 Redis String 자료구조를 사용한다.

반면 인기 검색어는 검색어별 조회 횟수를 score로 관리하고 높은 순서대로 TOP 10을 조회해야 하므로 Redis Sorted Set을 사용한다.

또한 V2와 V3는 서로 다른 캐시 저장소를 사용하므로 cache name과 CacheManager를 명확히 분리한다.

V2는 Caffeine Local Cache를 사용하므로 JVM 메모리에 `Page<ProductListItemResponse>` 객체를 그대로 저장할 수 있다.

하지만 V3는 Redis Remote Cache를 사용하므로 JSON 직렬화와 역직렬화를 고려해야 한다.

따라서 Redis에는 Spring 내부 객체인 `Page`, `PageImpl`, `Pageable`, `Sort`를 직접 저장하지 않고, 직접 정의한 응답 DTO인 `ProductPageResponse`를 저장한다.

Redis key에는 `StringRedisSerializer`를 사용하고, value에는 타입을 명확히 지정할 수 있는 `JacksonJsonRedisSerializer<ProductPageResponse>`를 사용한다.

캐시 최신성 관점에서는 TTL 5분만으로는 상품 변경 직후의 최신성을 보장할 수 없으므로, 상품 변경 시 `@CacheEvict(allEntries = true)`로 V2와 V3 검색 캐시를 즉시 무효화한다.

`@CachePut`은 갱신 대상 key가 명확한 상품 상세 캐시에는 적합할 수 있지만, 검색 결과 캐시는 keyword, category, status, sort, page, size 조합별로 key가 생성되어 상품 하나가 어떤 key에 포함되어 있는지 알기 어렵다.

따라서 이번 프로젝트에서는 `@CachePut`보다 전체 검색 캐시를 삭제하는 `@CacheEvict` 방식을 선택했다.

이를 통해 첫 번째 요청에서는 DB 조회 후 Redis에 검색 결과를 저장하고, 두 번째 동일 요청에서는 Redis에 저장된 검색 결과를 정상적으로 역직렬화하여 반환할 수 있다.

또한 상품 변경 이후에는 기존 검색 캐시를 즉시 제거하여 다음 검색 요청이 최신 DB 기준으로 다시 캐시를 생성하도록 한다.

Redis 서버의 `maxmemory-policy`는 현재 설정하지 않는다.

검색 결과 캐시만 고려하면 TTL이 설정된 key 중 만료 시간이 가까운 key를 먼저 제거하는 `volatile-ttl`이 적합할 수 있다.

하지만 Redis의 `maxmemory-policy`는 Redis 인스턴스 전체에 적용되는 설정이다.

현재 프로젝트에서는 Redis를 검색 캐시뿐 아니라 Refresh Token, Logout Token 같은 인증 관련 데이터에도 사용할 수 있고, 인증 관련 key 역시 TTL을 가진다.

따라서 `volatile-ttl`을 전체 Redis에 적용하면 메모리 부족 상황에서 인증 관련 key가 조기 제거될 가능성이 있다.

검색 캐시는 제거되더라도 DB 조회를 통해 다시 생성할 수 있지만, 인증 관련 key가 제거되면 토큰 재발급 실패나 로그아웃 토큰 무효화 같은 문제가 발생할 수 있다.

따라서 현재 구현에서는 Redis 서버의 `maxmemory-policy`를 별도로 설정하지 않고, 검색 캐시는 TTL 5분과 상품 변경 시 즉시 무효화로 관리한다.

추후 운영 환경에서 검색 캐시용 Redis와 인증용 Redis를 분리할 경우, 검색 캐시 전용 Redis에 `volatile-ttl` 정책 적용을 검토한다.
