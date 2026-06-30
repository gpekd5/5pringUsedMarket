# RDBMS와 NoSQL의 차이 정리

## 1. 학습 배경

검색 V3 API에서 Caffeine Local Cache를 Redis Remote Cache로 확장하면서 Redis를 사용하게 되었다.

Redis는 일반적인 RDBMS처럼 테이블과 관계를 중심으로 데이터를 저장하는 방식이 아니라, Key-Value 기반으로 데이터를 저장하는 NoSQL 저장소이다.

이번 프로젝트에서는 MySQL과 Redis를 함께 사용한다.

```text
MySQL
→ 회원, 상품, 검색 로그, 관심상품 등 영속 데이터 저장

Redis
→ 검색 결과 캐시, 인기검색어 집계, 토큰 관리 등 빠른 조회가 필요한 데이터 저장
```

따라서 Redis를 단순히 캐시 도구로만 사용하는 것이 아니라, RDBMS와 NoSQL이 어떤 차이를 가지는지 이해할 필요가 있었다.

---

## 2. RDBMS란?

RDBMS는 Relational Database Management System의 약자이다.

즉, 관계형 데이터베이스 관리 시스템을 의미한다.

대표적인 RDBMS는 다음과 같다.

```text
MySQL
PostgreSQL
Oracle
MariaDB
SQL Server
```

RDBMS는 데이터를 테이블 형태로 저장한다.

예를 들어 회원과 상품 데이터는 다음과 같이 테이블로 관리할 수 있다.

```text
members
 ├── id
 ├── email
 ├── nickname
 └── role

products
 ├── id
 ├── member_id
 ├── title
 ├── price
 ├── category
 └── status
```

상품은 판매자인 회원과 관계를 가진다.

```text
members.id = products.member_id
```

이처럼 RDBMS는 데이터 간 관계를 명확하게 표현하고, 정합성을 유지하는 데 강점이 있다.

---

## 3. NoSQL이란?

NoSQL은 Not Only SQL의 의미로 사용된다.

RDBMS처럼 고정된 테이블 구조와 SQL 중심으로만 데이터를 다루는 것이 아니라, 다양한 방식으로 데이터를 저장하는 데이터베이스를 말한다.

대표적인 NoSQL 유형은 다음과 같다.

| 유형            | 설명                                | 예시               |
| ------------- | --------------------------------- | ---------------- |
| Key-Value     | key 하나에 value 하나를 저장한다.           | Redis, DynamoDB  |
| Document      | JSON 문서 형태로 데이터를 저장한다.            | MongoDB          |
| Column Family | 컬럼 단위로 데이터를 저장하고 대량 데이터 처리에 적합하다. | Cassandra, HBase |
| Graph         | 노드와 간선으로 관계를 표현한다.                | Neo4j            |

이번 프로젝트에서 사용한 Redis는 NoSQL 중에서도 Key-Value 저장소에 해당한다.

Redis는 다음과 같이 데이터를 저장한다.

```text
key   = productSearchV3::keyword=맥북:category=DIGITAL:page=0:size=10
value = 검색 결과 JSON
```

즉, 테이블과 조인보다는 특정 key를 기준으로 value를 빠르게 조회하는 데 초점이 있다.

---

## 4. RDBMS와 NoSQL의 핵심 차이

RDBMS와 NoSQL의 가장 큰 차이는 데이터 저장 방식과 사용 목적이다.

| 구분     | RDBMS                 | NoSQL                                |
| ------ | --------------------- | ------------------------------------ |
| 데이터 구조 | 테이블, 행, 열             | Key-Value, Document, Column, Graph 등 |
| 스키마    | 고정된 스키마 중심            | 유연한 구조                               |
| 조회 방식  | SQL 사용                | 저장소별 API 또는 명령어 사용                   |
| 관계 표현  | Foreign Key, Join에 강함 | 관계보다는 빠른 조회나 확장성에 강함                 |
| 정합성    | 강한 정합성에 적합            | 성능과 확장성을 우선하는 경우가 많음                 |
| 대표 사용처 | 핵심 비즈니스 데이터 저장        | 캐시, 로그, 세션, 실시간 집계, 대용량 데이터 처리       |
| 예시     | MySQL, PostgreSQL     | Redis, MongoDB, Cassandra            |

정리하면 RDBMS는 관계와 정합성이 중요한 데이터에 적합하고, NoSQL은 빠른 조회나 유연한 데이터 저장이 필요한 경우에 적합하다.

---

## 5. 프로젝트에서 RDBMS를 사용하는 이유

이번 중고거래 프로젝트에서는 MySQL을 RDBMS로 사용한다.

MySQL에 저장하는 주요 데이터는 다음과 같다.

```text
회원
상품
상품 이미지
검색 로그
관심상품
쿠폰
채팅방
채팅 메시지
```

이 데이터들은 서로 관계를 가진다.

예를 들어 상품은 판매자인 회원과 연결된다.

```text
members 1 : N products
```

관심상품은 회원과 상품의 관계를 나타낸다.

```text
members N : M products
→ wishes 테이블로 연결
```

검색 로그는 특정 회원이 어떤 검색어를 검색했는지 저장한다.

```text
members 1 : N search_logs
```

이런 데이터는 다음과 같은 이유로 RDBMS에 저장하는 것이 적합하다.

| 이유       | 설명                                |
| -------- | --------------------------------- |
| 관계 표현 필요 | 회원, 상품, 관심상품, 검색 로그는 서로 관계가 있다.   |
| 정합성 필요   | 존재하지 않는 회원의 상품이 등록되면 안 된다.        |
| 트랜잭션 필요  | 상품 상태 변경, 쿠폰 발급 등은 데이터 일관성이 중요하다. |
| 조건 검색 필요 | 상품 목록, 검색, 필터링, 정렬, 페이징이 필요하다.    |
| 영속성 필요   | 서버가 꺼져도 데이터가 보존되어야 한다.            |

따라서 프로젝트의 핵심 비즈니스 데이터는 MySQL에 저장한다.

---

## 6. 프로젝트에서 Redis를 사용하는 이유

이번 프로젝트에서는 Redis를 다음 기능에 사용한다.

```text
검색 결과 캐시
인기 검색어 집계
Refresh Token 저장
로그아웃 토큰 처리
```

Redis를 사용하는 이유는 다음과 같다.

| 사용 목적    | Redis를 선택한 이유                            |
| -------- | ---------------------------------------- |
| 검색 결과 캐시 | 동일 검색 조건의 반복 요청에서 DB 조회를 줄일 수 있다.        |
| 인기 검색어   | Sorted Set을 사용하면 score 기준 TOP 10 조회가 쉽다. |
| 토큰 관리    | TTL을 설정하여 만료 시간을 Redis에서 관리할 수 있다.       |
| 서버 확장    | 여러 서버가 동일한 Redis 데이터를 공유할 수 있다.          |

Redis는 메모리 기반 저장소이기 때문에 조회 속도가 빠르다.

또한 TTL을 쉽게 설정할 수 있어 일정 시간이 지나면 데이터가 자동으로 만료되도록 관리할 수 있다.

예를 들어 검색 결과 캐시는 다음과 같이 관리한다.

```text
key   = productSearchV3::keyword=맥북:category=DIGITAL:status=ON_SALE:page=0:size=10
value = ProductPageResponse JSON
TTL   = 5분
```

이 구조를 사용하면 동일한 검색 조건의 요청이 반복될 때 DB를 다시 조회하지 않고 Redis에서 검색 결과를 반환할 수 있다.

---

## 7. Redis가 RDBMS를 대체하지 않는 이유

Redis는 빠른 조회에 강하지만, RDBMS를 완전히 대체하기 위한 저장소는 아니다.

예를 들어 상품 데이터를 Redis에만 저장한다고 가정하면 다음 문제가 발생할 수 있다.

```text
상품과 회원의 관계 관리 어려움
복잡한 조건 검색 어려움
트랜잭션 처리 제한
데이터 정합성 관리 어려움
영속성 정책 별도 고려 필요
```

중고거래 프로젝트에서 상품 데이터는 단순 key-value가 아니다.

상품은 회원, 이미지, 관심상품, 채팅 등 여러 데이터와 연결된다.

또한 상품 검색은 다음과 같은 조건을 조합해야 한다.

```text
keyword
category
status
sort
page
size
```

이런 조건 검색과 관계 관리는 RDBMS와 QueryDSL이 더 적합하다.

따라서 Redis는 원본 데이터 저장소가 아니라, MySQL 조회 결과를 빠르게 재사용하기 위한 캐시 저장소로 사용한다.

```text
MySQL
→ 원본 데이터 저장소

Redis
→ 반복 조회 최적화를 위한 캐시 저장소
```

---

## 8. 검색 기능에서 RDBMS와 Redis의 역할 분리

검색 V3 API에서는 MySQL과 Redis의 역할을 다음과 같이 분리했다.

```text
MySQL
→ 상품 원본 데이터 저장
→ QueryDSL로 조건 검색 수행
→ 정렬, 필터링, 페이징 처리

Redis
→ 동일 검색 조건의 결과 캐싱
→ Cache Hit 시 DB 조회 생략
```

검색 요청 흐름은 다음과 같다.

```text
검색 요청
→ Redis Cache 확인
→ Cache Hit: Redis 검색 결과 반환
→ Cache Miss: MySQL 조회
→ QueryDSL 검색 수행
→ ProductPageResponse 생성
→ Redis에 저장
→ 응답 반환
```

즉 Redis는 검색 조건을 직접 해석하거나 상품 데이터를 직접 필터링하지 않는다.

검색 조건 처리는 MySQL과 QueryDSL이 담당하고, Redis는 이미 만들어진 검색 결과 응답을 저장하고 재사용한다.

---

## 9. Key-Value 저장 방식 이해

Redis는 Key-Value 구조로 데이터를 저장한다.

가장 단순한 형태는 다음과 같다.

```text
key   = name
value = hong
```

검색 결과 캐시에서는 key에 검색 조건을 포함한다.

```text
key = productSearchV3::keyword=Mac:category=DIGITAL:status=ON_SALE:sortType=LATEST:page=0:size=10
```

value에는 검색 결과 응답 DTO를 JSON으로 저장한다.

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

이렇게 저장하면 같은 key로 요청이 들어왔을 때 Redis가 value를 바로 반환할 수 있다.

즉 Key-Value 저장소는 “어떤 key로 데이터를 찾을 것인가”가 중요하다.

검색 결과 캐시에서는 검색 결과에 영향을 주는 조건을 모두 key에 포함해야 한다.

```text
keyword
category
status
sortType
page
size
pageable sort
```

이 중 하나라도 빠지면 서로 다른 검색 요청이 같은 캐시 key를 공유하게 되어 잘못된 검색 결과가 반환될 수 있다.

---

## 10. Redis 자료구조와 프로젝트 적용

Redis는 단순 String 외에도 다양한 자료구조를 제공한다.

이번 프로젝트에서는 기능에 따라 다른 자료구조를 사용한다.

| 기능            | Redis 자료구조 | 선택 이유                                                 |
| ------------- | ---------- | ----------------------------------------------------- |
| 검색 결과 캐시      | String     | 검색 조건 key에 검색 결과 JSON 하나를 저장하면 되기 때문이다.               |
| 인기 검색어        | Sorted Set | 검색어별 score를 증가시키고 높은 순서대로 TOP 10을 조회해야 하기 때문이다.       |
| Refresh Token | String     | memberId 또는 token key에 token 값을 저장하면 되기 때문이다.         |
| 로그아웃 토큰       | String     | Access Token을 key로 저장하고 TTL 동안 블랙리스트처럼 관리할 수 있기 때문이다. |

검색 결과 캐시는 String을 사용한다.

```text
SET productSearchV3::keyword=Mac... "{...검색 결과 JSON...}"
```

인기 검색어는 Sorted Set을 사용한다.

```text
ZINCRBY popular:keywords 1 "Mac"
ZREVRANGE popular:keywords 0 9 WITHSCORES
```

이처럼 Redis는 단순히 하나의 저장 방식만 제공하는 것이 아니라, 기능 목적에 맞는 자료구조를 선택할 수 있다는 장점이 있다.

---

## 11. RDBMS와 Redis를 함께 사용할 때 주의할 점

RDBMS와 Redis를 함께 사용할 때는 캐시 정합성을 고려해야 한다.

Redis는 빠른 조회를 위해 MySQL의 조회 결과를 저장한다.

하지만 MySQL의 원본 데이터가 변경되었는데 Redis 캐시가 그대로 남아 있으면 오래된 데이터가 반환될 수 있다.

예를 들어 상품 상태가 변경되었다고 가정한다.

```text
상품 A 상태 변경
ON_SALE → SOLD
```

하지만 Redis에 기존 검색 결과가 남아 있으면 다음 검색 요청에서 오래된 상태의 상품이 노출될 수 있다.

```text
검색 요청
→ Redis Cache Hit
→ 기존 캐시 결과 반환
→ SOLD 변경 전 데이터 노출 가능
```

따라서 상품 변경 시 검색 캐시를 무효화해야 한다.

이번 프로젝트에서는 상품 등록, 수정, 삭제, 상태 변경 시 검색 캐시를 전체 삭제한다.

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

즉 RDBMS와 Redis를 함께 사용할 때는 “Redis를 어떻게 저장할 것인가”뿐 아니라 “원본 데이터가 변경되었을 때 캐시를 어떻게 지울 것인가”도 함께 고려해야 한다.

---

## 12. RDBMS와 NoSQL 선택 기준

RDBMS와 NoSQL 중 무엇이 더 좋은지는 상황에 따라 다르다.

중요한 것은 데이터의 성격에 맞는 저장소를 선택하는 것이다.

| 상황                             | 적합한 저장소           |
| ------------------------------ | ----------------- |
| 데이터 관계가 중요하다.                  | RDBMS             |
| 트랜잭션과 정합성이 중요하다.               | RDBMS             |
| 복잡한 조건 검색과 조인이 필요하다.           | RDBMS             |
| 서버가 꺼져도 반드시 보존되어야 하는 핵심 데이터이다. | RDBMS             |
| 단순 key로 빠르게 조회하고 싶다.           | NoSQL Key-Value   |
| TTL로 자동 만료가 필요하다.              | Redis             |
| 실시간 순위 집계가 필요하다.               | Redis Sorted Set  |
| 대량 로그나 유연한 JSON 문서 저장이 필요하다.   | Document 기반 NoSQL |

이번 프로젝트 기준으로 정리하면 다음과 같다.

```text
회원, 상품, 검색 로그, 관심상품
→ 관계와 정합성이 중요하므로 MySQL

검색 결과 캐시, 인기 검색어, 토큰
→ 빠른 조회와 TTL, 자료구조 활용이 중요하므로 Redis
```

---

## 13. 정리

RDBMS는 관계와 정합성이 중요한 데이터를 저장하는 데 적합하다.

NoSQL은 데이터 구조가 더 유연하고, 빠른 조회나 확장성이 필요한 상황에 적합하다.

이번 프로젝트에서는 MySQL과 Redis를 각각 다른 목적으로 사용했다.

```text
MySQL
→ 원본 데이터 저장소
→ 회원, 상품, 검색 로그, 관심상품 등 핵심 데이터 관리

Redis
→ 보조 저장소
→ 검색 결과 캐시, 인기 검색어, 토큰 관리
```

Redis는 빠르지만 MySQL을 완전히 대체하는 것이 아니라, MySQL 조회 결과를 빠르게 재사용하거나 TTL이 필요한 데이터를 관리하는 역할로 사용한다.

검색 V3 API를 구현하면서 RDBMS와 NoSQL은 경쟁 관계가 아니라, 데이터 성격에 따라 역할을 나누어 함께 사용할 수 있다는 점을 확인했다.
