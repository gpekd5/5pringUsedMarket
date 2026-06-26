# [Troubleshooting] 상품 검색에서 왜 동적 QueryDSL을 사용할까?

## 1. 배경

이번 프로젝트에서는 중고거래 상품 검색 기능을 구현해야 했다.

검색 API는 단순히 검색어만 조회하는 것이 아니라 다음과 같은 다양한 검색 조건을 지원해야 했다.

```text
검색어(keyword)

카테고리(category)

상품 상태(status)

정렬(sort)

페이징(page, size)
```

예를 들어 아래와 같은 요청들이 모두 가능해야 했다.

```text
GET /products/search?keyword=맥북

GET /products/search?category=DIGITAL

GET /products/search?keyword=맥북&category=DIGITAL

GET /products/search?keyword=맥북&sort=PRICE_ASC&page=0
```

즉 검색 조건의 조합이 계속 증가하는 **동적 검색(Dynamic Search)** 기능이 필요했다.

---

## 2. 문제

처음에는 Spring Data JPA의 메서드 이름 기반 조회를 고려했다.

예를 들어

```java
findByTitleContaining(...)
```

처럼 간단한 검색은 쉽게 구현할 수 있다.

하지만 검색 조건이 하나씩 늘어날수록 메서드 수도 함께 증가하게 된다.

```text
keyword

keyword + category

keyword + status

keyword + category + status

keyword + category + sort

...
```

조건 조합마다 Repository 메서드를 만들어야 하므로 유지보수가 어려워질 것이라고 판단했다.

---

## 3. 해결 방법 검토

동적 검색을 구현하기 위해 다음과 같은 방법을 비교했다.

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| Spring Data JPA Method | 구현이 가장 간단하다. | 조건 조합이 많아질수록 메서드가 계속 증가한다. |
| JPQL | 복잡한 Query 작성이 가능하다. | 문자열 기반이라 유지보수가 어렵다. |
| JPA Specification | Predicate 기반 동적 검색이 가능하다. | 코드가 길고 가독성이 떨어질 수 있다. |
| QueryDSL | 타입 안정성과 동적 조건 처리에 적합하다. | 초기 설정이 필요하다. |

---

## 4. QueryDSL을 선택한 이유

이번 검색 기능은 앞으로

```text
V1

↓

V2 (Cache)

↓

V3 (Redis)
```

까지 확장될 예정이었다.

따라서 검색 로직 자체는 계속 재사용할 수 있어야 했다.

QueryDSL은

- 타입 안정성
- DTO Projection
- 동적 조건 처리
- OrderSpecifier를 이용한 정렬

등을 자연스럽게 지원하였다.

또한 조건 메서드를 분리하기 쉬워 유지보수성이 높다고 판단하였다.

---

## 5. BooleanBuilder와 BooleanExpression 비교

QueryDSL에서도 동적 조건을 구현하는 방식은 여러 가지가 있다.

### BooleanBuilder

```java
BooleanBuilder builder = new BooleanBuilder();

if(keyword != null){
    builder.and(product.title.contains(keyword));
}

if(category != null){
    builder.and(product.category.eq(category));
}
```

장점

- 구현이 직관적이다.

단점

- 조건이 많아질수록 코드가 길어진다.
- 조건 메서드를 재사용하기 어렵다.

---

### BooleanExpression

```java
.where(
    keyword(keyword),
    category(category),
    status(status)
)
```

각 조건은 메서드로 분리한다.

```java
private BooleanExpression keyword(String keyword){
    ...
}
```

장점

- 조건을 메서드 단위로 분리할 수 있다.
- null 조건을 자동으로 무시한다.
- 재사용성이 높다.
- 가독성이 좋다.

---

## 6. 이번 프로젝트에서 선택한 방식

이번 프로젝트에서는 **BooleanExpression** 방식을 선택하였다.

선택 이유는 다음과 같다.

- 조건 메서드를 재사용할 수 있다.
- 새로운 검색 조건이 추가되어도 메서드 하나만 추가하면 된다.
- Repository 코드가 간결해진다.
- QueryDSL이 null 조건을 자동으로 제외해준다.

실제 검색 조건은 다음과 같이 분리하였다.

```text
keyword()

category()

status()
```

이를 통해 필요한 조건만 조합하여 Query를 생성하도록 구현하였다.

---

## 7. 적용 결과

검색 Repository는 다음과 같은 구조가 되었다.

```text
SearchService

↓

ProductSearchRepository

↓

BooleanExpression

↓

QueryDSL

↓

DB
```

검색 조건이 증가하더라도 기존 코드를 크게 수정하지 않고 조건 메서드만 추가하면 되도록 개선하였다.

또한 V2 캐시 기능에서도 동일한 Repository를 그대로 재사용할 수 있었다.

---

## 8. 느낀 점

처음에는 단순히 QueryDSL을 사용하는 것이 목적이라고 생각했다.

하지만 구현을 진행하면서 중요한 것은 **QueryDSL 자체가 아니라, 증가하는 검색 조건을 어떻게 유지보수하기 쉬운 구조로 만들 것인가**라는 점을 알게 되었다.

QueryDSL은 이러한 문제를 해결하기 위한 수단이었고,

특히 BooleanExpression을 활용하여 조건을 메서드 단위로 분리한 구조가 이후 V2(Caffeine Cache), V3(Redis Cache)에서도 그대로 재사용될 수 있다는 점이 가장 큰 장점이었다.