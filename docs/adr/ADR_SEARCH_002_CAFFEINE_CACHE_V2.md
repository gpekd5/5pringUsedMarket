# 검색 V2 API 캐시 설계 결정

## 1. 캐시 적용 배경

상품 검색 API는 사용자가 상품을 찾기 위해 반복적으로 호출할 가능성이 높은 기능이다.

예를 들어 인기 키워드나 특정 카테고리 검색은 여러 사용자가 비슷한 조건으로 반복 요청할 수 있다.

```text
GET /api/v1/products/search?keyword=맥북&page=0&size=10
GET /api/v1/products/search?keyword=맥북&page=0&size=10
GET /api/v1/products/search?keyword=맥북&page=0&size=10
```

기존 v1 검색 API는 요청이 들어올 때마다 DB를 조회한다.

```text
검색 요청
→ DB 조회
→ 결과 응답
```

같은 조건의 검색 요청이 반복되면 매번 동일한 DB 조회가 발생할 수 있다.

따라서 v2 검색 API에서는 동일한 검색 조건의 반복 요청에 대해 캐시를 적용하여 DB 조회 횟수를 줄이고자 했다.

---

## 2. 캐시 구현체 선택지 비교

검색 결과 캐시를 구현하기 위해 다음 방식들을 검토했다.

| 방법 | 설명 | 장점 | 단점 |
| --- | --- | --- | --- |
| 직접 Map 사용 | `Map` 또는 `ConcurrentHashMap`에 검색 결과를 직접 저장 | 구현 원리를 이해하기 쉽다. 외부 라이브러리가 필요 없다. | TTL, 최대 크기, 만료 정책을 직접 구현해야 한다. 운영 환경에서 안정적으로 관리하기 어렵다. |
| Spring Cache + Simple Cache | Spring Boot 기본 캐시 추상화 사용 | 설정이 간단하다. `@Cacheable`을 쉽게 적용할 수 있다. | 실제 운영용 캐시로는 기능이 부족하다. TTL, maximumSize 같은 세부 제어가 어렵다. |
| Spring Cache + Caffeine | Spring Cache 추상화에 Caffeine을 캐시 구현체로 사용 | `@Cacheable`과 쉽게 연동된다. TTL, maximumSize 설정이 가능하다. 로컬 메모리 기반이라 빠르다. | 서버 메모리를 사용하므로 서버 간 캐시 공유가 불가능하다. Scale-out 환경에서는 한계가 있다. |
| Redis Cache | Redis를 캐시 저장소로 사용 | 여러 서버가 캐시를 공유할 수 있다. Scale-out 환경에 적합하다. TTL 관리가 가능하다. | Redis 설정과 직렬화 설정이 필요하다. 네트워크 통신 비용이 있다. 로컬 캐시보다 구조가 복잡하다. |

---

## 3. 이번 프로젝트에서 필요한 수준

이번 프로젝트의 검색 v2 요구사항은 다음과 같다.

```text
기존 v1 검색 API는 유지한다.
새로운 v2 검색 API에 In-memory Cache를 적용한다.
Local Memory Cache를 사용한다.
TTL과 maximumSize를 설정한다.
@Cacheable을 활용한다.
```

즉 이번 단계에서 필요한 것은 Redis 기반 분산 캐시가 아니라, **애플리케이션 내부 메모리를 사용하는 로컬 캐시**이다.

또한 현재 프로젝트는 학습용 팀 프로젝트이며, 단일 서버 환경을 기준으로 개발하고 있다.

따라서 이번 v2 검색 API에서는 다음 조건이 중요했다.

| 기준 | 필요성 |
| --- | --- |
| 구현 난이도 | 팀 프로젝트 기간 안에 적용 가능해야 한다. |
| Spring 연동성 | `@Cacheable`을 활용해 기존 Service 구조에 자연스럽게 적용해야 한다. |
| TTL 설정 | 상품 정보 변경 가능성을 고려해 캐시가 영구히 남지 않아야 한다. |
| 최대 크기 제한 | 검색 조건 조합이 많아질 수 있으므로 메모리 사용량을 제한해야 한다. |
| 성능 개선 효과 | 동일 검색 조건 반복 요청 시 DB 조회를 줄일 수 있어야 한다. |

이 기준으로 보면 직접 Map 구현은 관리 기능이 부족하고, Redis Cache는 이번 요구사항보다 무겁다.

따라서 이번 프로젝트에서는 **Spring Cache + Caffeine Local Cache**를 선택했다.

---

## 4. 캐시 구현체 결정

이번 검색 v2 API에서는 **Caffeine Local Cache**를 사용한다.

```text
캐시 구현체
→ Caffeine Local Cache
```

선택 이유는 다음과 같다.

| 선택 이유 | 설명 |
| --- | --- |
| 요구사항 적합성 | In-memory Cache, Local Memory Cache 요구사항에 맞다. |
| Spring Cache 연동 | `@Cacheable`을 통해 Spring AOP 기반으로 쉽게 적용할 수 있다. |
| TTL 설정 가능 | 캐시 만료 시간을 설정할 수 있어 오래된 검색 결과 노출을 줄일 수 있다. |
| maximumSize 설정 가능 | 캐시 개수를 제한해 서버 메모리 사용량을 제어할 수 있다. |
| 구현 복잡도 적절 | Redis Cache보다 단순하고, 직접 Map 구현보다 안정적이다. |
| 현재 인프라 수준 적합 | 현재 프로젝트는 단일 서버 기준이므로 Local Cache로 충분하다. |

정리하면 이번 프로젝트에서는 **검색 결과 캐시 v2의 목적이 분산 캐시 구축이 아니라, 동일 검색 조건 반복 요청의 DB 조회를 줄이는 것**이므로 Caffeine Local Cache가 적절하다고 판단했다.

---

## 5. TTL 설정 결정

캐시 만료 시간은 **5분**으로 설정한다.

```text
TTL
→ 5분
```

TTL 설정 후보는 다음과 같이 검토했다.

| TTL 후보 | 장점 | 단점 |
| --- | --- | --- |
| 30초 | 데이터 신선도가 높다. 상품 변경 사항이 빠르게 반영된다. | 캐시 유지 시간이 짧아 반복 검색 요청에 대한 캐시 효과가 작다. |
| 1분 | 비교적 최신 데이터를 유지할 수 있다. | 인기 검색 조건의 반복 요청을 충분히 흡수하기에는 짧을 수 있다. |
| 5분 | 캐시 효과와 데이터 신선성 사이의 균형이 좋다. 반복 검색 요청에 대한 DB 부하를 줄일 수 있다. | 상품 수정/삭제/상태 변경이 즉시 반영되지 않을 수 있다. |
| 30분 이상 | 캐시 효과가 크다. DB 조회를 크게 줄일 수 있다. | 오래된 상품 정보가 노출될 가능성이 커진다. 검색 결과 신뢰도가 떨어질 수 있다. |

상품 검색 결과는 다음과 같은 데이터 변경에 영향을 받는다.

```text
상품 등록
상품 수정
상품 삭제
상품 상태 변경
가격 변경
판매완료 처리
```

따라서 캐시를 너무 오래 유지하면 오래된 검색 결과가 노출될 수 있다.

하지만 검색 결과는 결제나 재고 차감처럼 강한 정합성이 필요한 데이터는 아니므로, 짧은 시간의 지연은 허용 가능하다고 판단했다.

이번 프로젝트에서는 **캐시 효과와 데이터 신선성의 균형**을 위해 TTL을 5분으로 설정한다.

---

## 6. maximumSize 설정 결정

캐시 최대 개수는 **500개**로 설정한다.

```text
maximumSize
→ 500
```

검색 결과 캐시는 검색 조건 조합별로 저장된다.

검색 결과에 영향을 주는 조건은 다음과 같다.

```text
keyword
category
status
sortType
page
size
pageable sort
```

예를 들어 아래 요청들은 모두 서로 다른 검색 결과를 만들 수 있다.

```text
keyword=맥북&page=0&size=10
keyword=맥북&page=1&size=10
keyword=아이폰&page=0&size=10
category=DIGITAL&page=0&size=10
keyword=맥북&sort=PRICE_ASC&page=0&size=10
```

즉 검색 조건 조합이 늘어나면 캐시 key도 계속 늘어난다.

maximumSize 후보는 다음과 같이 검토했다.

| maximumSize 후보 | 장점 | 단점 |
| --- | --- | --- |
| 제한 없음 | 캐시 hit 가능성이 높아질 수 있다. | 검색 조건이 많아지면 서버 메모리를 계속 사용한다. 메모리 관리가 어렵다. |
| 100개 | 메모리 사용량을 작게 유지할 수 있다. | 검색 조건이 조금만 다양해져도 캐시가 자주 밀려 캐시 효과가 낮을 수 있다. |
| 500개 | 학습용 팀 프로젝트 규모에서 반복 검색 조건을 캐시에 유지하면서 메모리 사용량을 제한할 수 있다. | 서비스 규모가 커지면 부족할 수 있다. |
| 1000개 이상 | 다양한 검색 조건을 더 오래 유지할 수 있다. | 현재 프로젝트 규모에 비해 다소 과하고, 서버 메모리 사용량이 커질 수 있다. |

이번 프로젝트는 대규모 운영 서비스가 아니라 학습용 팀 프로젝트이며, 검색 조건 캐시를 무제한으로 보관할 필요는 없다.

따라서 **메모리 사용량을 제한하면서도 반복 검색 요청에 대한 캐시 효과를 확인할 수 있는 수준**으로 maximumSize를 500개로 설정한다.

---

## 7. 캐시 전략 선택

검색 v2 API에서는 **Cache-aside 전략**을 사용한다.

캐시 전략 후보는 다음과 같다.

| 전략 | 설명 | 장점 | 단점 | 검색 API 적합성 |
| --- | --- | --- | --- | --- |
| Cache-aside | 캐시에 데이터가 있으면 캐시를 사용하고, 없으면 DB 조회 후 캐시에 저장 | 읽기 요청에 적합하다. 구현이 단순하다. `@Cacheable` 기본 동작과 잘 맞다. | 최초 요청은 DB를 조회해야 한다. 데이터 변경 시 캐시 무효화 전략이 필요하다. | 적합 |
| Write-through | 데이터를 쓸 때 DB와 캐시에 동시에 반영 | DB와 캐시의 정합성을 유지하기 쉽다. | 쓰기 성능이 느려질 수 있다. 검색 조회 기능에는 과하다. | 부적합 |
| Write-back | 먼저 캐시에 쓰고 나중에 DB에 반영 | 쓰기 성능을 높일 수 있다. | 데이터 유실이나 정합성 문제가 발생할 수 있다. 구현이 복잡하다. | 부적합 |
| Refresh-ahead | 캐시 만료 전에 미리 갱신 | 자주 조회되는 데이터에 대해 안정적인 캐시 hit를 기대할 수 있다. | 구현이 복잡하다. 현재 프로젝트 규모에는 과하다. | 과함 |

상품 검색 API는 데이터를 저장하거나 수정하는 기능이 아니라 **읽기 중심 기능**이다.

따라서 캐시에 같은 검색 조건의 결과가 있으면 캐시에서 반환하고, 없으면 DB를 조회한 뒤 캐시에 저장하는 Cache-aside 방식이 가장 적합하다.

Spring의 `@Cacheable`은 기본적으로 Cache-aside 방식으로 동작한다.

```text
요청
→ 캐시에 같은 key가 있는지 확인
→ 캐시에 있으면 캐시 응답
→ 캐시에 없으면 DB 조회
→ DB 조회 결과를 캐시에 저장
→ 응답
```

---

## 8. 캐시 key 설계

검색 결과 캐시의 key에는 **검색 결과에 영향을 주는 모든 조건**이 포함되어야 한다.

검색 결과에 영향을 주는 값은 다음과 같다.

| 값 | key에 포함해야 하는 이유 |
| --- | --- |
| keyword | 검색어가 다르면 결과가 달라진다. |
| category | 카테고리가 다르면 결과가 달라진다. |
| status | 판매 상태 필터가 다르면 결과가 달라진다. |
| sortType | 검색 정렬 조건이 다르면 결과 순서가 달라진다. |
| page | 페이지 번호가 다르면 반환되는 상품 목록이 달라진다. |
| size | 페이지 크기가 다르면 반환되는 상품 개수가 달라진다. |
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
keyword={keyword}:category={category}:status={status}:sortType={sortType}:page={page}:size={size}:sort={pageableSort}
```

실제 구현에서는 key 문자열이 길어지는 것을 피하고, Redis 전환 시에도 재사용할 수 있도록 별도 Key Generator를 사용한다.

```java
@Cacheable(
        cacheNames = "productSearch",
        key = "@searchCacheKeyGenerator.generate(#condition, #pageable)"
)
```

`SearchCacheKeyGenerator`는 검색 조건과 페이징 정보를 조합해 다음과 같은 key를 생성한다.

```text
keyword=맥북:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10:sort=UNSORTED
```

여기서 `cacheNames`와 `key`의 역할은 다르다.

```text
cacheNames
→ 캐시 저장소 이름
→ productSearch

key
→ 캐시 저장소 안에서 특정 검색 결과를 구분하는 식별자
→ keyword=맥북:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10:sort=UNSORTED
```

즉 구조는 다음과 같다.

```text
productSearch
 └── keyword=맥북:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10:sort=UNSORTED
     └── ProductPageResponse
```

---

## 9. 검색어 기록과 캐시 분리

검색 v2에서 주의할 점은 **검색어 기록 저장과 상품 검색 결과 캐시를 분리해야 한다는 점**이다.

이번 프로젝트의 검색 정책은 다음과 같다.

```text
상품 검색
→ 로그인/비로그인 모두 가능

검색어 기록 저장
→ 로그인 사용자의 keyword 검색만 저장

인기검색어 집계
→ 로그인 사용자의 keyword 검색만 Redis ZSet에 반영
```

그런데 `@Cacheable`이 붙은 메서드 내부에서 검색어 기록을 저장하면 문제가 생길 수 있다.

```java
@Cacheable(...)
public ProductPageResponse searchProductsV2(...) {
    saveSearchLog(member, keyword);
    return productSearchRepository.search(...);
}
```

캐시 hit가 발생하면 `@Cacheable`이 적용된 메서드 자체가 실행되지 않는다.

```text
캐시 hit
→ 메서드 실행 안 됨
→ saveSearchLog 실행 안 됨
→ 최근 검색어 저장 안 됨
→ 인기검색어 집계 안 됨
```

하지만 사용자가 검색 API를 호출했다면 캐시 hit 여부와 상관없이 검색 행위는 기록되어야 한다.

따라서 이번 프로젝트에서는 검색어 기록과 상품 검색 결과 캐시를 분리한다.

```text
SearchService
→ 검색 정책 처리
→ 검색어 기록 저장
→ 인기검색어 Redis ZSet 집계
→ 캐시가 적용된 검색 조회 호출

CachedProductSearchReader
→ @Cacheable 적용
→ 상품 검색 결과 조회
```

이 구조를 사용하면 검색어 기록은 캐시 hit 여부와 상관없이 수행되고, 상품 검색 결과 조회만 캐시의 영향을 받는다.

---

## 10. Local Cache의 한계

Caffeine Local Cache는 애플리케이션 서버 내부 메모리에 캐시 데이터를 저장한다.

따라서 단일 서버 환경에서는 빠르고 단순하게 사용할 수 있다.

하지만 서버가 여러 대로 확장되면 각 서버가 서로 다른 캐시를 가지게 된다.

```text
요청 1
→ 서버 A
→ 서버 A의 Local Cache에 저장

요청 2
→ 서버 B
→ 서버 B에는 해당 캐시 없음
→ 다시 DB 조회
```

즉 Local Cache는 서버 간 데이터 공유가 불가능하다.

| 구분 | Local Cache | Redis Cache |
| --- | --- | --- |
| 저장 위치 | 애플리케이션 서버 내부 메모리 | 외부 Redis 서버 |
| 조회 속도 | 매우 빠름 | 네트워크 통신이 필요하지만 빠름 |
| 구현 복잡도 | 낮음 | 상대적으로 높음 |
| 서버 간 공유 | 불가능 | 가능 |
| Scale-out 적합성 | 낮음 | 높음 |

이번 프로젝트의 v2 검색 API는 Local Cache 적용을 목표로 하므로 Caffeine을 사용한다.

다만 추후 서버가 여러 대로 확장되거나, 캐시를 서버 간 공유해야 한다면 Redis 기반 Remote Cache로 개선할 수 있다.

---

## 11. 구현 반영 내용

구현에서는 다음 파일을 기준으로 캐시를 적용했다.

```text
CacheConfig
→ CaffeineCacheManager 설정
→ cacheName: productSearch
→ TTL: 5분
→ maximumSize: 500

SearchCacheKeyGenerator
→ keyword, category, status, sortType, page, size, pageable sort 기반 key 생성

SearchService
→ 검색 조건 파싱
→ 검색 로그 저장
→ 인기검색어 Redis ZSet 집계
→ CachedProductSearchReader 호출

CachedProductSearchReader
→ @Cacheable 적용
→ 상품 검색 결과 조회
```

V2 검색 흐름은 다음과 같다.

```text
/api/v2/products/search 요청
→ SearchFacade
→ SearchService.searchProductsV2()
→ 검색 조건 파싱
→ 검색 로그 저장
→ 인기검색어 Redis ZSet 집계
→ CachedProductSearchReader.search()
→ Cache Hit 여부 확인
→ Hit: 캐시 결과 반환
→ Miss: DB 조회 후 캐시 저장
→ ProductPageResponse 응답
```

---

## 12. 최종 설계 결정

이번 검색 v2 캐시 설계는 다음과 같이 결정한다.

| 항목 | 결정 |
| --- | --- |
| 캐시 적용 API | `/api/v2/products/search` |
| 기존 API 유지 | `/api/v1/products/search` 유지 |
| 캐시 구현체 | Caffeine Local Cache |
| 캐시 전략 | Cache-aside |
| 캐시 저장소 이름 | `productSearch` |
| TTL | 5분 |
| maximumSize | 500개 |
| 캐시 key | keyword + category + status + sortType + page + size + pageable sort |
| 캐시 key 생성 | `SearchCacheKeyGenerator` |
| 검색어 기록 저장 | 캐시와 분리하여 항상 처리 |
| 인기검색어 집계 | 캐시와 분리하여 항상 처리 |
| 캐시 적용 클래스 | `CachedProductSearchReader` |
| 한계 | 서버 간 캐시 공유 불가 |

정리하면 이번 프로젝트에서는 **동일 검색 조건의 반복 요청에 대한 DB 부하를 줄이는 것**이 목적이다.

현재 프로젝트는 단일 서버 환경을 기준으로 하고 있고, 요구사항 또한 Local Memory Cache 적용을 요구하고 있다.

따라서 Redis Cache보다 구현이 단순하고 Spring Cache와 쉽게 연동되는 Caffeine Local Cache를 선택했다.

또한 검색 조건 조합이 과도하게 쌓이는 것을 막기 위해 maximumSize를 500개로 제한하고, 상품 정보 변경 가능성을 고려해 TTL은 5분으로 설정했다.