# TROUBLESHOOTING: LIKE 선두 와일드카드 인덱스 미적용

## 증상

`products` 테이블에 인덱스를 추가했음에도 키워드 검색 시 `EXPLAIN` 결과가 개선되지 않음.

```
products → type: ALL, key: NULL, rows: 49,134, Extra: Using where; Using filesort
```

카테고리·상태 필터만 있을 때는 인덱스가 적용되지만, 키워드를 함께 입력하면 여전히 Full Table Scan이 발생한다.

## 원인

`ProductSearchRepository.search` 쿼리의 키워드 검색 조건에 선두 와일드카드(`%`)가 사용되고 있기 때문이다.

```java
// ProductSearchRepository.java
private BooleanExpression keywordContains(String keyword) {
    return StringUtils.hasText(keyword)
            ? product.title.contains(keyword)  // → LIKE '%keyword%'
            : null;
}
```

QueryDSL의 `.contains()`는 내부적으로 `LIKE '%keyword%'`로 변환된다. B-Tree 인덱스는 값의 **앞부분부터 순서대로** 저장하기 때문에, 선두 와일드카드가 있으면 어느 위치에서도 일치할 수 있어 인덱스 범위 탐색이 불가능하다. 결국 옵티마이저는 인덱스를 포기하고 전체 행을 스캔한다.

```
LIKE 'keyword%'  → 인덱스 사용 가능 (앞부분 고정)
LIKE '%keyword%' → 인덱스 사용 불가 (앞부분 불확정)
```

## 발생 배경

중고거래 플랫폼 특성상 상품 제목 어디에나 키워드가 포함될 수 있어 `LIKE '%keyword%'` 형태가 필요하다. 후방 와일드카드(`LIKE 'keyword%'`)로는 요구사항을 충족할 수 없기 때문에 구조적으로 B-Tree 인덱스를 사용할 수 없는 상황이다.

## 해결 방안

### 방법 1: FULLTEXT 인덱스 적용

**FULLTEXT 인덱스란?**
B-Tree 인덱스가 값의 앞부분부터 순서대로 탐색하는 것과 달리, FULLTEXT 인덱스는 텍스트를 단어(토큰) 단위로 분해해 역색인(inverted index) 구조로 저장한다. "노트북 충전기"라는 제목이 있으면 "노트북", "충전기"를 각각 인덱싱해 두고, 검색어가 어느 위치에 있든 빠르게 찾을 수 있다. `LIKE '%keyword%'`처럼 전체를 스캔하지 않아도 된다.

```sql
ALTER TABLE products ADD FULLTEXT INDEX ft_products_title (title);
```

```sql
-- LIKE '%keyword%' 대신
SELECT * FROM products WHERE MATCH(title) AGAINST('keyword' IN BOOLEAN MODE);
```

단, 한국어는 영어처럼 띄어쓰기로 단어를 구분할 수 없어 기본 파서로는 한 글자 검색이 안 된다. 한국어 지원을 위해 `ngram` 파서를 사용해야 하며, MySQL 설정에서 최소 토큰 길이(`ngram_token_size`)를 조정해야 한다. 또한 FULLTEXT 인덱스는 INSERT/UPDATE/DELETE 시 역색인을 재구성하므로 B-Tree 인덱스보다 쓰기 비용이 더 크다.

### 방법 2: Elasticsearch 도입

**Elasticsearch란?**
Apache Lucene 기반의 분산 검색 엔진으로, 텍스트 전문 검색에 최적화되어 있다. 형태소 분석, 유사어 처리, 오타 보정, 검색 결과 관련도 점수 계산 등 FULLTEXT 인덱스보다 훨씬 정교한 검색 기능을 제공한다. 데이터를 MySQL에 저장하면서 검색용 사본을 Elasticsearch에 별도로 인덱싱해 두고, 검색 요청만 Elasticsearch로 위임하는 방식으로 사용한다.

MySQL FULLTEXT 인덱스 대비 장점은 대용량 데이터에서의 검색 성능, 한국어 형태소 분석기(Nori) 기본 지원, 분산 처리로 수평 확장이 가능하다는 점이다. 단, Elasticsearch 클러스터를 별도로 운영해야 하고 MySQL과의 데이터 동기화 로직이 필요해 운영 복잡도가 크게 높아진다.

### 방법 3: 현 상태 유지 (현재 적용 중)

키워드 검색은 Full Table Scan을 감수하되, **카테고리·상태 필터만 있는 경우**에는 `idx_products_category_status_created_at` 인덱스가 적용되도록 설계한다. 키워드 없이 카테고리 필터만 사용하는 경우가 더 빈번하다고 판단하여 해당 경로에서 인덱스 효과를 극대화한다.

## 현재 상태

방법 3을 유지 중. 키워드 검색 시 Full Table Scan은 불가피하며, 이는 `LIKE '%keyword%'` 형태의 구조적 한계다. 데이터가 수백만 건 이상으로 증가하거나 검색 응답 속도가 문제가 되는 시점에 방법 1(FULLTEXT) 또는 방법 2(Elasticsearch) 도입을 검토한다.
