# 인덱스 설계 및 성능 분석

`EXPLAIN` 실행 계획 분석을 통해 `type: ALL`(Full Table Scan), `Using filesort`, `Using temporary`가 확인된 쿼리를 대상으로 인덱스를 설계하여 적용합니다.

---

## 병목 쿼리 식별

| 쿼리 | 대상 | 병목 원인 |
|------|------|-----------|
| `ProductSearchRepository.search` — 목록 조회 | 상품 검색 목록 | `type: ALL`, `Using filesort` |
| `ProductSearchRepository.search` — COUNT 조회 | 상품 검색 총 건수 | `type: ALL` (Full Table Scan) |
| `ChatRoomRepository.findRoomsByMember` | 내 채팅방 목록 | `type: ALL`, `Using filesort`, JOIN 인덱스 미사용 |

---

## 적용 인덱스 DDL

### `ProductSearchRepository.search`

```sql
-- products: 카테고리 필터(등호) → 상태 필터 → 최신순 정렬을 하나의 인덱스로 커버
-- Leftmost Prefix Rule: 등호 조건(category)을 선두에 배치
CREATE INDEX idx_products_category_status_created_at
    ON products (category, status, created_at DESC);

-- product_images: LEFT JOIN ON product_id = ? AND sort_order = 0 커버
-- Leftmost Prefix Rule: JOIN 등호 조건(product_id) → 필터 등호 조건(sort_order)
CREATE INDEX idx_product_images_product_id_sort_order
    ON product_images (product_id, sort_order);
```

> **한계**: 키워드 검색 조건인 `LIKE '%keyword%'`는 선두 와일드카드(`%`) 때문에 B-Tree 인덱스를 사용할 수 없다. 인덱스는 값의 앞부분부터 순서대로 탐색하는데, 선두 와일드카드는 어디서든 일치할 수 있으므로 인덱스 범위 탐색이 불가능하다. 따라서 키워드가 입력된 경우 `products` Full Table Scan은 인덱스로 해결할 수 없는 구조적 한계다. 설계한 인덱스는 **카테고리·상태 필터만 있고 키워드가 없는 경우**에 효과적이다.

### `ChatRoomRepository.findRoomsByMember`

```sql
-- chat_members: WHERE member_id = ? 조건과 JOIN chat_rooms ON chat_room_id = cr.id 를 커버
-- Leftmost Prefix Rule: WHERE 등호 조건(member_id)을 선두에 배치
-- 기존 uq_chat_member(chat_room_id, member_id)는 순서가 반대라 member_id 단독 조회 시 인덱스 미사용
CREATE INDEX idx_chat_members_member_id_chat_room_id
    ON chat_members (member_id, chat_room_id);

-- chat_rooms: ORDER BY last_message_at DESC 정렬을 인덱스로 처리
-- ※ 아래 한계 참고
CREATE INDEX idx_chat_rooms_last_message_at
    ON chat_rooms (last_message_at DESC);
```

> **한계**: 실제 쿼리의 `ORDER BY`가 `CASE WHEN cr.last_message_at IS NULL THEN 1 ELSE 0 END ASC, cr.last_message_at DESC` 표현식을 사용하기 때문에 `idx_chat_rooms_last_message_at` 인덱스가 정렬에 적용되지 않는다. B-Tree 인덱스는 컬럼 값을 직접 정렬할 때만 사용 가능하며, 표현식 계산 결과 기준 정렬에는 사용할 수 없다. `chat_rooms`의 `Using filesort`는 인덱스로 제거 불가능한 구조적 한계다.

---

## 인덱스 적용 전/후 성능 비교

**측정 환경**: MySQL 8.0 (Docker), 더미 데이터 **5만 건**, `SET profiling = 1` / `SHOW PROFILES` 사용

### `ProductSearchRepository.search` — 목록 조회 쿼리

```sql
SELECT p.id, p.title, p.price, p.category, p.status, pi.image_url, p.created_at
FROM products p
LEFT JOIN product_images pi ON pi.product_id = p.id AND pi.sort_order = 0
WHERE p.status != 'DELETED'
  AND p.category = 'DIGITAL'
ORDER BY p.created_at DESC
LIMIT 10 OFFSET 0;
```

| 항목 | Before (인덱스 없음) | After (인덱스 적용) | 개선 |
|------|---------------------|---------------------|------|
| `type` | `ALL` (Full Table Scan) | `ref` | ✓ |
| `key` | `NULL` | `idx_products_category_status_created_at` | ✓ |
| `rows` | 49,134 | 5,001 | **90% 감소** |
| `Extra` | `Using where; Using filesort` | `Using index condition` | `Using filesort` 제거 |
| 실행 시간 | 71.9 ms | 16.7 ms | **77% 개선** |

### `ProductSearchRepository.search` — COUNT 쿼리

```sql
SELECT COUNT(p.id)
FROM products p
WHERE p.status != 'DELETED'
  AND p.category = 'DIGITAL';
```

| 항목 | Before (인덱스 없음) | After (인덱스 적용) | 개선 |
|------|---------------------|---------------------|------|
| `type` | `ALL` (Full Table Scan) | `ref` | ✓ |
| `key` | `NULL` | `idx_products_category_status_created_at` | ✓ |
| `rows` | 49,134 | 5,001 | **90% 감소** |
| 실행 시간 | 15.9 ms | 2.3 ms | **85% 개선** |

---

## 설계 원칙

- **Leftmost Prefix Rule**: 복합 인덱스에서 등호 조건(`=`) 컬럼을 선두에 배치하고, 범위 조건 → 정렬 순서로 구성
- **FK 자동 인덱스**: `@ManyToOne` + `@JoinColumn` 선언 시 MySQL이 FK 컬럼 단일 인덱스를 자동 생성하므로 중복 선언 불필요
- **LIKE 선두 와일드카드**: `LIKE '%keyword%'` 형태는 B-Tree 인덱스 사용 불가 — 해당 쿼리는 키워드 검색 특성상 Full Table Scan이 불가피

---

## Q&A

### Q1. 왜 이 쿼리를 최적화 대상으로 선정했나요?

**`ProductSearchRepository.search`**

상품 검색은 비로그인 사용자 포함 모든 요청에서 호출되는 메인 페이지 진입점이다. 페이징된 목록 조회와 전체 건수 COUNT가 매 요청마다 함께 실행되며, 카테고리·상태 필터와 최신순 정렬이 조합된다. 데이터가 늘어날수록 필터 조건 없이 전체를 읽는 구조라면 선형적으로 느려지기 때문에 가장 먼저 개선 대상으로 선정했다.

**`ChatRoomRepository.findRoomsByMember`**

채팅방 목록은 로그인한 사용자가 채팅 탭 진입 시마다 호출된다. `chat_members`와 `chat_rooms`를 JOIN하고 최근 메시지 기준으로 정렬하는 구조로, 채팅방 수가 많아질수록 정렬 비용이 증가한다. 실시간 서비스 특성상 반복 호출 빈도가 높아 잠재적 병목으로 판단했다.

---

### Q2. EXPLAIN으로 무엇을 발견했나요?

**`ProductSearchRepository.search` — Before**

```
products       → type: ALL,    key: NULL,           rows: 49,134  Extra: Using where; Using filesort
product_images → type: ALL,    key: NULL,           rows: 1       Extra: Using join buffer (hash join)
```

- `products` Full Table Scan: 카테고리·상태 필터가 있음에도 인덱스가 없어 전체 행을 읽음
- `Using filesort`: `ORDER BY created_at DESC` 처리를 위해 인덱스 없이 정렬 수행
- `Using join buffer (hash join)`: `product_images` JOIN 시 인덱스 미사용, 메모리 내 해시 조인으로 처리

**`ChatRoomRepository.findRoomsByMember` — Before**

```
chat_rooms   → type: ALL,    key: NULL,              rows: 50,668  Extra: Using filesort
chat_members → type: eq_ref, key: uq_chat_member,   rows: 1       Extra: Using index
```

- `chat_rooms` Full Table Scan: `last_message_at` 인덱스 없이 전체 행을 읽은 후 정렬
- `Using filesort`: `CASE WHEN last_message_at IS NULL` 표현식 정렬로 filesort 발생
- `chat_members`는 기존 유니크 인덱스 `uq_chat_member(chat_room_id, member_id)`로 동작 중이나, 이 순서로는 `WHERE member_id = ?` 단독 조건에서 인덱스를 활용하지 못함

---

### Q3. 인덱스를 어떻게 설계했고, 왜 그 컬럼에?

**`idx_products_category_status_created_at` — (category, status, created_at DESC)**

Leftmost Prefix Rule 적용: 복합 인덱스에서 등호 조건 컬럼을 선두에 배치해야 인덱스가 최대한 활용된다.

| 순서 | 컬럼 | 조건 타입 | 선택 이유 |
|------|------|-----------|-----------|
| 1 | `category` | 등호(`=`) | 카디널리티 중간, 등호 조건이므로 선두 배치 |
| 2 | `status` | 부등호(`!=`) | category로 필터된 결과를 추가로 좁힘 |
| 3 | `created_at DESC` | 정렬 | `ORDER BY created_at DESC`를 인덱스로 처리해 filesort 제거 |

`status`는 `!= 'DELETED'` 부등호 조건이지만, `category` 등호 조건 이후에 배치하면 인덱스 범위 내에서 추가 필터링이 가능하다. 만약 `status`를 선두에 배치하면 `category` 조건이 인덱스를 활용하지 못한다.

**`idx_product_images_product_id_sort_order` — (product_id, sort_order)**

```sql
LEFT JOIN product_images pi ON pi.product_id = p.id AND pi.sort_order = 0
```

JOIN 조건이 `product_id = ?` + `sort_order = 0` 두 등호 조건이다. `product_id`가 FK이자 JOIN 키이므로 선두에 배치하고, `sort_order`를 두 번째로 두어 대표 이미지 필터까지 인덱스 내에서 처리한다.

**`idx_chat_members_member_id_chat_room_id` — (member_id, chat_room_id)**

기존 유니크 인덱스 `uq_chat_member(chat_room_id, member_id)`는 `chat_room_id`가 선두라 `WHERE member_id = ?` 단독 조건에서 인덱스를 사용할 수 없다. `member_id`를 선두로 한 별도 인덱스를 추가해 특정 회원의 채팅방 조회를 인덱스로 처리한다.

**`idx_chat_rooms_last_message_at` — (last_message_at DESC)**

`ORDER BY last_message_at DESC` 정렬을 인덱스로 처리하려는 의도로 설계했다. 그러나 실제 쿼리가 `CASE WHEN` 표현식을 사용하기 때문에 인덱스가 적용되지 않는다. (Q4 참고)

---

### Q4. 성능이 얼마나 개선됐나요?

**`ProductSearchRepository.search` — 목록 조회**: 71.9ms → 16.7ms (**77% 개선**), rows 49,134 → 5,001 (**90% 감소**), `Using filesort` 제거

**`ProductSearchRepository.search` — COUNT 쿼리**: 15.9ms → 2.3ms (**85% 개선**), rows 49,134 → 5,001 (**90% 감소**)

**`ChatRoomRepository.findRoomsByMember`**:

| 항목 | Before | After | 비고 |
|------|--------|-------|------|
| `chat_rooms` type | `ALL` | `ALL` | `CASE WHEN` 표현식으로 인해 변화 없음 |
| `chat_members` possible_keys | `uq_chat_member` | `uq_chat_member`, `idx_chat_members_member_id_chat_room_id` | 신규 인덱스 인식 |
| 실행 시간 | 61.6 ms | 57.3 ms | 유의미한 개선 없음 |

`chat_members` 인덱스는 추가됐으나 드라이빙 테이블이 `chat_rooms`(Full Scan)이기 때문에 `chat_members`는 `chat_room_id` 기준으로 JOIN되어 기존 `uq_chat_member`가 선택된다. `chat_rooms`의 `Using filesort`와 Full Scan은 쿼리 구조 변경 없이는 인덱스로 해결이 불가능하다.

---

### Q5. 인덱스의 부작용은?

**INSERT / UPDATE 성능 영향**

인덱스는 읽기 성능을 높이는 대신 쓰기 시 B-Tree 재구성 비용이 발생한다. 행이 INSERT/UPDATE/DELETE될 때마다 인덱스 파일도 함께 갱신되므로, 인덱스 수가 많을수록 쓰기 성능이 저하된다.

| 인덱스 | 영향 받는 쓰기 |
|--------|--------------|
| `idx_products_category_status_created_at` | 상품 등록, 상태 변경 |
| `idx_product_images_product_id_sort_order` | 이미지 업로드, 순서 변경 |
| `idx_chat_members_member_id_chat_room_id` | 채팅방 참여 |
| `idx_chat_rooms_last_message_at` | 메시지 전송마다 `last_message_at` 갱신 |

특히 `last_message_at`은 메시지 전송마다 갱신되므로 채팅이 활발할수록 인덱스 재구성 빈도가 높다. 현재 쿼리 구조에서 이 인덱스가 실제로 사용되지 않기 때문에 쓰기 비용만 발생하고 읽기 이득은 없는 상태다.

**인덱스 과다 생성의 trade-off**

- 인덱스마다 디스크 공간을 추가로 점유한다.
- 옵티마이저가 여러 인덱스 중 하나를 선택해야 하므로 실행 계획 수립 시간이 늘어난다.
- 인덱스가 많을수록 통계 정보 갱신(`ANALYZE TABLE`) 비용도 증가한다.

따라서 실제로 사용되지 않는 인덱스는 제거하는 것이 낫다. `idx_chat_rooms_last_message_at`은 현재 쿼리에서 효과가 없으므로, 쿼리 구조를 바꾸지 않는다면 제거를 검토해야 한다. 근본적인 해결책은 `ORDER BY CASE WHEN` 표현식을 제거하고 `last_message_at`에 초기값을 설정해 `NULL`이 발생하지 않도록 쿼리와 엔티티를 수정하는 것이다.
