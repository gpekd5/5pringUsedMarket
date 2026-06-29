# 검색 V1 API 설계 결정

## 1. 검색 API 구현 배경

상품 검색 기능은 중고거래 서비스의 핵심 기능 중 하나이다.

사용자는 다양한 조건으로 상품을 검색할 수 있어야 하며, 검색 조건에 따라 결과가 유동적으로 변경된다.

이번 프로젝트에서 검색 결과에 영향을 주는 조건은 다음과 같다.

```text
keyword
category
status
sort
page
size
```

예를 들어 다음과 같은 요청이 모두 가능해야 한다.

```text
GET /api/v1/products/search?keyword=맥북
GET /api/v1/products/search?category=DIGITAL
GET /api/v1/products/search?keyword=맥북&category=DIGITAL
GET /api/v1/products/search?keyword=맥북&sort=PRICE_ASC
```

즉 검색 조건의 조합이 계속 증가하는 **동적 검색(Dynamic Search)** 기능이 필요했다.

---

## 2. 검색 구현 방식 검토

검색 기능 구현을 위해 다음과 같은 방식을 검토하였다.

| 방법 | 설명 | 장점 | 단점 |
| --- | --- | --- | --- |
| Spring Data JPA Method | 메서드 이름으로 Query 생성 | 구현이 단순하다. | 조건이 많아질수록 메서드가 폭발적으로 증가한다. |
| JPQL | 문자열 기반 Query 작성 | 복잡한 Query 작성이 가능하다. | 문자열 기반이라 컴파일 시 검증이 어렵고 유지보수가 어렵다. |
| JPA Specification | Predicate 기반 동적 검색 | Spring Data JPA와 잘 연동된다. | 코드가 길고 가독성이 떨어질 수 있다. |
| QueryDSL | 타입 안전성을 제공하는 Query Builder | 동적 조건을 유연하게 조합할 수 있다. DTO 조회와 정렬이 편리하다. | 초기 설정이 필요하다. |

---

## 3. 이번 프로젝트에서 필요한 수준

검색 API는 다음 조건을 만족해야 했다.

```text
검색어 검색

카테고리 필터

판매 상태 필터

정렬

페이징

조건 조합
```

또한 앞으로 검색 기능은

```text
V1 검색

↓

V2 캐시 검색

↓

V3 Redis 검색
```

으로 확장될 예정이었다.

따라서 검색 로직을 재사용하기 쉬운 구조가 필요했다.

이번 프로젝트에서는 다음 요소를 중요하게 고려하였다.

| 기준 | 필요성 |
| --- | --- |
| 동적 조건 처리 | 검색 조건 조합이 계속 증가한다. |
| 타입 안정성 | 컴파일 시 Query 오류를 확인할 수 있어야 한다. |
| 유지보수성 | 조건 메서드를 분리하여 재사용 가능해야 한다. |
| DTO 조회 | Entity 대신 DTO Projection이 가능해야 한다. |
| 정렬 지원 | 다양한 정렬 조건을 쉽게 추가할 수 있어야 한다. |

---

## 4. QueryDSL 선택

이번 프로젝트에서는 **QueryDSL**을 선택하였다.

선택 이유는 다음과 같다.

| 선택 이유 | 설명 |
| --- | --- |
| 동적 검색 적합 | BooleanExpression을 이용하여 조건을 자유롭게 조합할 수 있다. |
| 타입 안정성 | 문자열이 아닌 QClass 기반으로 컴파일 시 오류를 확인할 수 있다. |
| 유지보수성 | 조건 메서드를 분리하여 재사용하기 쉽다. |
| DTO 조회 | Projections를 통해 원하는 데이터만 조회할 수 있다. |
| 정렬 확장 | OrderSpecifier를 활용하여 정렬 조건을 쉽게 추가할 수 있다. |
| 확장성 | V2 캐시, V3 Redis에서도 동일 Repository를 재사용할 수 있다. |

---

## 5. BooleanExpression 선택

QueryDSL에서도 동적 조건을 구현하는 방식은 여러 가지가 있다.

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| BooleanBuilder | 조건을 하나씩 추가하기 쉽다. | 코드가 길어지고 재사용성이 떨어질 수 있다. |
| BooleanExpression | 조건 메서드를 분리하기 쉽다. null을 자동으로 무시한다. | 처음에는 구조를 이해하는 데 시간이 필요하다. |

이번 프로젝트에서는 **BooleanExpression** 방식을 선택하였다.

예를 들어 조건 메서드를 각각 분리하였다.

```text
keyword()

category()

status()
```

필요한 조건만 반환하고,

null인 조건은 QueryDSL이 자동으로 제외한다.

```java
.where(
    keyword(condition.getKeyword()),
    category(condition.getCategory()),
    status(condition.getStatus())
)
```

따라서 조건이 추가되더라도 기존 코드를 크게 수정하지 않고 확장할 수 있다.

---

## 6. 정렬 구현 방식

검색 결과는 다음과 같은 정렬을 지원해야 한다.

```text
LATEST

PRICE_ASC

PRICE_DESC

OLDEST
```

정렬 구현 방식도 여러 가지를 검토하였다.

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| if-else | 구현이 쉽다. | 조건이 많아질수록 코드가 길어진다. |
| OrderSpecifier | QueryDSL과 자연스럽게 연동된다. | QueryDSL 의존성이 있다. |

이번 프로젝트에서는 **OrderSpecifier**를 사용하였다.

정렬 조건을 메서드로 분리하여 관리하였다.

---

## 7. 검색 Repository 구조

검색 기능은 전용 Repository로 분리하였다.

```text
SearchService

↓

ProductSearchRepository

↓

QueryDSL

↓

DB
```

검색 전용 Repository를 분리함으로써

일반 CRUD Repository와 검색 Query를 분리할 수 있었다.

---

## 8. 구현 반영 내용

구현에서는 다음과 같은 구조를 적용하였다.

```text
SearchFacade

↓

SearchService

↓

ProductSearchRepository

↓

BooleanExpression

↓

OrderSpecifier

↓

DTO Projection

↓

ProductPageResponse
```

검색 조건은 DTO로 관리하였다.

```text
ProductSearchCondition
```

이를 통해 검색 조건이 늘어나더라도 메서드 파라미터를 계속 추가하지 않고 관리할 수 있도록 설계하였다.

---

## 9. 향후 확장 방향

검색 Repository는 V2에서도 그대로 재사용한다.

```text
V1

SearchService
↓

ProductSearchRepository
↓

DB

↓

V2

SearchService
↓

CachedProductSearchReader
↓

ProductSearchRepository
↓

DB
```

검색 로직은 그대로 유지하면서 캐시 계층만 추가할 수 있도록 설계하였다.

향후 V3에서는 Redis Cache를 추가하더라도 Repository는 변경하지 않는 것을 목표로 한다.

---

## 10. 최종 설계 결정

| 항목 | 결정 |
| --- | --- |
| 검색 구현 방식 | QueryDSL |
| 동적 조건 처리 | BooleanExpression |
| 정렬 | OrderSpecifier |
| 검색 조건 객체 | ProductSearchCondition |
| 응답 객체 | ProductPageResponse |
| 검색 Repository | ProductSearchRepository |
| 조회 방식 | DTO Projection |
| 확장 방향 | V2(Caffeine) → V3(Redis)에서도 Repository 재사용 |

정리하면 이번 프로젝트에서는 **검색 조건이 계속 증가하는 동적 검색 기능을 유지보수하기 쉽도록 구현하는 것**을 목표로 하였다.

이를 위해 QueryDSL과 BooleanExpression을 사용하여 조건을 메서드 단위로 분리하였으며, DTO Projection과 OrderSpecifier를 함께 활용해 가독성과 확장성을 높였다.

또한 검색 로직을 전용 Repository로 분리하여 이후 V2 캐시와 V3 Redis 캐시에서도 동일한 검색 로직을 재사용할 수 있도록 설계하였다.