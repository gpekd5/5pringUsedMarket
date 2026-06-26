# 중고 커머스 팀프로젝트 ERD 기준 개발 가이드

> 목적: Codex/AI 에이전트가 현재 ERD를 기준으로 엔티티, 제약조건, API, 예외처리 방향을 일관되게 생성하도록 하기 위한 기준 문서입니다.  
> 서비스 컨셉: 당근마켓형 중고거래 플랫폼 + 상품 검색/인기검색어 + 선착순 쿠폰 이벤트 + 구매자-판매자 채팅 + CS 문의 채팅

---

## 1. 전체 도메인 구성

현재 ERD는 다음 도메인으로 구성한다.

| 도메인 | 주요 테이블 | 설명 |
|---|---|---|
| 회원 | `members` | 서비스 사용자 및 관리자 계정 |
| 상품 | `products`, `product_images`, `wishes` | 중고 상품 게시글, 이미지, 찜 |
| 검색 | `search_logs` | 사용자 검색 기록 및 인기 검색어 집계 기반 |
| 쿠폰 | `coupons`, `user_coupons` | 선착순 이벤트 쿠폰 및 사용자 발급 이력 |
| 채팅 | `chat_rooms`, `chat_members`, `chat_messages` | 거래 채팅 및 CS 문의 채팅 |

---

## 2. 공통 개발 원칙

### 2.1 엔티티 기본 원칙

- 모든 테이블의 PK는 `BIGINT` 기반 `id`를 사용한다.
- JPA 엔티티에서는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`를 기본으로 사용한다.
- 생성일과 수정일을 모두 가진 엔티티는 `BaseEntity`를 상속한다.
- `BaseEntity`는 현재 `created_at`, `updated_at`을 관리하며, 추후 소프트 삭제 정책이 확정되면 `deleted_at`을 추가할 수 있도록 확장 가능한 공통 엔티티로 둔다.
- 이력성 테이블처럼 `created_at`만 가진 엔티티는 생성 시간만 별도로 매핑한다.
- `created_at`, `updated_at`은 ERD에 정의된 컬럼만 `LocalDateTime`으로 매핑한다.
- 외래키 컬럼은 ERD 기준으로 유지하되, JPA 연관관계는 기본적으로 `@ManyToOne(fetch = FetchType.LAZY)`를 사용한다.
- 불필요한 양방향 연관관계는 만들지 않는다.
- 컬렉션 `@OneToMany`는 필요한 경우에만 제한적으로 사용한다.

### 2.2 Enum 관리 원칙

DB에는 문자열 Enum으로 저장한다.

```java
@Enumerated(EnumType.STRING)
```

사용할 Enum은 다음과 같다.

```java
public enum MemberRole {
    MEMBER, ADMIN
}

public enum ProductCategory {
    DIGITAL, FURNITURE, CLOTHING, BOOK, SPORTS, KIDS, BEAUTY, FOOD, PET, ETC
}

public enum ProductStatus {
    ON_SALE, RESERVED, SOLD, DELETED
}

public enum ChatRoomType {
    TRADE, CS
}

public enum ChatMemberRole {
    MEMBER, ADMIN
}

public enum CsStatus {
    WAITING, IN_PROGRESS, COMPLETED
}
```

---

## 3. 테이블별 상세 설계

## 3.1 members

### 역할

서비스 회원 정보를 저장한다. 일반 회원과 관리자를 `role`로 구분한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 회원 PK |
| email | VARCHAR(100) | 로그인 이메일 |
| password | VARCHAR(255) | 암호화된 비밀번호 |
| nickname | VARCHAR(50) | 닉네임 |
| role | ENUM(MEMBER, ADMIN) | 권한 |
| created_at | DATETIME | 생성일 |
| updated_at | DATETIME | 수정일 |

### 제약조건

- `email`은 이메일 형식만 허용하며, 유니크해야 한다.
- `nickname`은 서비스 내에서 유일해야 한다.
- `password`는 반드시 BCrypt 등 단방향 해시로 저장한다.

### 인덱스 추천

```sql
CREATE UNIQUE INDEX uk_members_email ON members(email);
CREATE UNIQUE INDEX uk_members_nickname ON members(nickname);
```

---

## 3.2 products

### 역할

판매자가 등록한 중고 상품 게시글을 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 상품 PK |
| member_id | BIGINT | 판매자 ID |
| title | VARCHAR(100) | 상품명 |
| price | INT | 가격 |
| description | TEXT | 설명 |
| category | ENUM | 카테고리 |
| status | ENUM(ON_SALE, RESERVED, SOLD, DELETED) | 판매 상태 |
| created_at | DATETIME | 생성일 |
| updated_at | DATETIME | 수정일 |

### 비즈니스 규칙

- 상품 등록자는 `member_id`의 회원이다.
- 상품 상태 변경은 판매자 본인만 가능하다.
- 구매 요청 테이블은 만들지 않는다.
- 구매 의사는 채팅으로만 진행한다.
- 거래 확정은 판매자가 상품 상태를 `RESERVED` 또는 `SOLD`로 변경하는 방식으로 처리한다.
- 삭제는 물리 삭제보다 `status = DELETED` 소프트 삭제를 우선한다.

### 상태 전이

```text
ON_SALE -> RESERVED
ON_SALE -> SOLD
RESERVED -> ON_SALE
RESERVED -> SOLD
ON_SALE -> DELETED
RESERVED -> DELETED
SOLD -> DELETED
```

주의:

- `SOLD -> ON_SALE`은 기본적으로 막는다.
- 운영상 복구가 필요하면 관리자 기능으로 별도 처리한다.

### 인덱스 추천

검색/목록 조회 성능을 위해 다음 인덱스를 우선 고려한다.

```sql
CREATE INDEX idx_products_status_created_at ON products(status, created_at);
CREATE INDEX idx_products_category_status_created_at ON products(category, status, created_at);
CREATE INDEX idx_products_member_status ON products(member_id, status);
```

상품명 LIKE 검색은 다음 조건을 기준으로 한다.

```sql
WHERE title LIKE CONCAT('%', :keyword, '%')
```

단, `%keyword%` 검색은 일반 B-Tree 인덱스를 잘 타지 못할 수 있으므로, 프로젝트 수준에서는 캐싱/더미데이터/EXPLAIN 비교 대상으로 활용한다.

---

## 3.3 product_images

### 역할

상품 이미지 S3 Object Key와 정렬 순서를 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 상품 이미지 PK |
| product_id | BIGINT | 상품 ID |
| image_key | VARCHAR(500) | Private S3 Object Key |
| sort_order | INT | 이미지 표시 순서 |
| created_at | DATETIME | 생성일 |

### 비즈니스 규칙

- 하나의 상품은 여러 이미지를 가질 수 있다.
- `sort_order = 0` 또는 가장 작은 값의 이미지를 대표 이미지로 사용한다.
- `product_images`에는 Public URL을 저장하지 않고 `image_key`만 저장한다.
- `image_key`는 서버가 업로드 응답으로 발급한 `products/{uuid}.{jpg|jpeg|png|webp}` 형식을 저장한다.
- 상품 상세/목록 응답에서는 저장된 `image_key`를 그대로 노출하지 않고 10분 만료 Presigned URL로 변환한다.
- 이미지 삭제/수정 시 상품 소유자 검증이 필요하다.

### image_url -> image_key 전환 메모

기존 운영 DB에 `product_images.image_url` 컬럼과 Public S3 URL 데이터가 있다면 배포 전에 `image_key` 컬럼을 추가하고 기존 URL의 path 부분을 key로 백필해야 한다.
이 저장소는 현재 Flyway/Liquibase를 사용하지 않으므로 전환 SQL은 배포 절차에서 명시적으로 실행한다.
참고 SQL은 `docs/db-migration/product-image-key-backfill.sql`에 둔다.

### 제약조건 추천

```sql
CREATE INDEX idx_product_images_product_sort ON product_images(product_id, sort_order);
```

---

## 3.4 wishes

### 역할

회원이 찜한 상품을 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 찜 PK |
| member_id | BIGINT | 회원 ID |
| product_id | BIGINT | 상품 ID |
| created_at | DATETIME | 생성일 |

### 비즈니스 규칙

- 같은 회원은 같은 상품을 한 번만 찜할 수 있다.
- 본인 상품 찜 허용 여부는 정책으로 정한다. 기본은 허용하지 않는 것을 권장한다.

### 제약조건

```sql
CREATE UNIQUE INDEX uk_wishes_member_product ON wishes(member_id, product_id);
CREATE INDEX idx_wishes_product ON wishes(product_id);
```

---

## 3.5 search_logs

### 역할

사용자 검색 기록을 저장한다. 인기 검색어 집계의 원천 데이터 또는 보조 데이터로 사용한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 검색 기록 PK |
| member_id | BIGINT | 검색한 회원 ID |
| keyword | VARCHAR(100) | 검색어 |
| created_at | DATETIME | 검색 시간 |

### 비즈니스 규칙

- 상품 검색 시 검색어를 저장한다.
- 인기 검색어는 DB 집계보다 Redis ZSet 사용을 우선한다.
- `search_logs`는 검색 이력 조회, 분석, Redis 복구용으로 사용한다.

### 인덱스 추천

```sql
CREATE INDEX idx_search_logs_keyword_created_at ON search_logs(keyword, created_at);
CREATE INDEX idx_search_logs_member_created_at ON search_logs(member_id, created_at);
```

---

## 3.6 coupons

### 역할

선착순 이벤트 쿠폰의 원본 정보를 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 쿠폰 PK |
| name | VARCHAR(200) | 쿠폰명 |
| total_qty | INT | 총 발급 가능 수량 |
| issued_qty | INT | 현재 발급 수량, 기본값 0 |
| event_start_at | DATETIME | 이벤트 시작 시간 |
| event_end_at | DATETIME | 이벤트 종료 시간 |
| expire_at | DATETIME | 쿠폰 만료 시간 |
| created_at | DATETIME | 생성일 |

### 비즈니스 규칙

- 쿠폰 예시: 첫 판매 등록 이벤트 아메리카노 쿠폰, 선착순 1000명.
- 쿠폰 수량 차감은 동시성 제어 대상이다.
- 잔여 수량은 `total_qty - issued_qty`로 판단한다.
- `issued_qty`는 0 이상 `total_qty` 이하여야 한다.

---

## 3.7 user_coupons

### 역할

회원에게 발급된 쿠폰 이력을 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 사용자 쿠폰 PK |
| member_id | BIGINT | 회원 ID |
| coupon_id | BIGINT | 쿠폰 ID |
| code | VARCHAR(100) | 쿠폰 코드 |
| issued_at | DATETIME | 발급 시간 |
| used_at | DATETIME | 사용 시간 |
| expire_at | DATETIME | 만료 시간 |

### 비즈니스 규칙

- 같은 회원은 같은 쿠폰을 한 번만 발급받을 수 있다.
- 쿠폰 발급은 Redis Lock으로 보호한다.
- 쿠폰 코드 중복 방지를 위해 `code`는 유니크로 관리한다.

### 제약조건

```sql
CREATE UNIQUE INDEX uk_user_coupons_member_coupon ON user_coupons(member_id, coupon_id);
CREATE UNIQUE INDEX uk_user_coupons_code ON user_coupons(code);
CREATE INDEX idx_user_coupons_member ON user_coupons(member_id);
```

### 동시성 처리 기준

쿠폰 발급 흐름:

```text
1. 사용자 쿠폰 발급 요청
2. Redis Lock 획득: lock:coupon:{couponId}
3. 이벤트 기간 검증
4. 중복 발급 여부 검증
5. 발급 수량 검증
6. issued_qty 증가
7. user_coupons 저장
8. Lock 해제
```

락 해제는 본인이 획득한 락만 해제하도록 UUID 값을 사용한다.

---

## 3.8 chat_rooms

### 역할

거래 채팅방 또는 CS 문의 채팅방을 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 채팅방 PK |
| type | ENUM(TRADE, CS) | 채팅방 유형 |
| title | VARCHAR(200) | 채팅방 제목 |
| product_id | BIGINT | 상품 전용 채팅방일 경우 상품 ID |
| cs_status | ENUM(WAITING, IN_PROGRESS, COMPLETED) | CS 문의 상태 |
| created_at | DATETIME | 생성일 |
| updated_at | DATETIME | 수정일 |

### 비즈니스 규칙

#### 거래 채팅방

- `type = TRADE`이면 `product_id`가 필요하다.
- 구매자와 판매자 간 채팅방은 같은 조합으로 중복 생성되면 안 된다.
- 단, 현재 `chat_rooms`만으로는 buyer/seller 중복을 직접 막기 어렵다.
- 중복 방지는 애플리케이션 로직 + `chat_members` 조회로 처리한다.

#### CS 문의 채팅방

- `type = CS`이면 `product_id`는 nullable 가능하다.
- CS 문의는 고객이 생성하고 관리자가 처리한다.
- `cs_status`는 `WAITING`으로 시작한다.

### CS 상태 전이

```text
WAITING -> IN_PROGRESS -> COMPLETED
WAITING -> COMPLETED
```

금지:

```text
COMPLETED -> WAITING
COMPLETED -> IN_PROGRESS
IN_PROGRESS -> WAITING
```

### 인덱스 추천

```sql
CREATE INDEX idx_chat_rooms_type_created_at ON chat_rooms(type, created_at);
CREATE INDEX idx_chat_rooms_product ON chat_rooms(product_id);
CREATE INDEX idx_chat_rooms_cs_status ON chat_rooms(cs_status);
```

---

## 3.9 chat_members

### 역할

채팅방 참여자 정보를 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 채팅 멤버 PK |
| chat_room_id | BIGINT | 채팅방 ID |
| member_id | BIGINT | 회원 ID |
| member_role | ENUM(MEMBER, ADMIN) | 채팅방 내 역할 |
| last_read_message_id | BIGINT | 마지막으로 읽은 메시지 ID |
| joined_at | DATETIME | 참여 시간 |

### 비즈니스 규칙

- 같은 회원은 같은 채팅방에 한 번만 참여할 수 있다.
- 읽지 않은 메시지 수는 `last_read_message_id` 기준으로 계산한다.
- `last_read_message_id`는 nullable 허용 가능하다.

### 제약조건

```sql
CREATE UNIQUE INDEX uk_chat_members_room_member ON chat_members(chat_room_id, member_id);
CREATE INDEX idx_chat_members_member ON chat_members(member_id);
CREATE INDEX idx_chat_members_room_role ON chat_members(chat_room_id, member_role);
```

---

## 3.10 chat_messages

### 역할

채팅 메시지를 저장한다.

### 컬럼

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | 메시지 PK |
| chat_room_id | BIGINT | 채팅방 ID |
| member_id | BIGINT | 보낸 회원 ID |
| content | TEXT | 메시지 내용 |
| created_at | DATETIME | 생성일 |

### 비즈니스 규칙

- 메시지는 반드시 채팅방에 속한다.
- 메시지는 반드시 발신 회원을 가진다.
- 클라이언트가 `senderId`를 보내더라도 신뢰하지 않는다.
- WebSocket/STOMP 인증에서 설정한 `Principal` 기준으로 발신자를 결정한다.
- 메시지 조회는 커서 기반 페이징을 사용한다.

### 조회 기준

```sql
SELECT *
FROM chat_messages
WHERE chat_room_id = :roomId
  AND id < :lastMessageId
ORDER BY id DESC
LIMIT :size;
```

### 인덱스 추천

```sql
CREATE INDEX idx_chat_messages_room_id_id ON chat_messages(chat_room_id, id);
CREATE INDEX idx_chat_messages_member_id ON chat_messages(member_id);
```

---

## 4. ERD 기준 JPA 연관관계 방향

## 4.1 기본 방향

실무 유지보수성을 위해 다음 방향을 기본으로 한다.

| 관계 | 권장 방향 | 이유 |
|---|---|---|
| Product -> Member | Product가 Member 참조 | 상품 조회 시 판매자 식별 필요 |
| ProductImage -> Product | ProductImage가 Product 참조 | 이미지 목록은 별도 Repository 조회 |
| Wish -> Member/Product | Wish가 둘 다 참조 | 찜 중복 제어 및 조회 용이 |
| SearchLog -> Member | SearchLog가 Member 참조 | 사용자 검색 이력 추적 |
| UserCoupon -> Member/Coupon | UserCoupon이 둘 다 참조 | 발급 이력 중심 설계 |
| ChatRoom -> Product | ChatRoom이 Product 참조 | 거래 채팅방 상품 연결 |
| ChatMember -> ChatRoom/Member | ChatMember가 둘 다 참조 | 참여자 중심 조회 |
| ChatMessage -> ChatRoom/Member | ChatMessage가 둘 다 참조 | 메시지 중심 설계 |

## 4.2 채팅 메시지는 양방향 금지

`ChatRoom`이 `List<ChatMessage>`를 직접 들고 있는 양방향 구조는 만들지 않는다.

이유:

- 채팅방 하나에 메시지가 수천~수만 건 쌓일 수 있다.
- 컬렉션 로딩 실수로 성능 문제가 발생하기 쉽다.
- 메시지는 항상 페이징으로 조회해야 한다.
- `ChatMessageRepository`에서 `chatRoomId` 기준으로 조회하는 방식이 유지보수에 유리하다.

---

## 5. 주요 API 설계 방향

## 5.1 인증/회원

| 기능 | Method | URL |
|---|---|---|
| 회원가입 | POST | `/api/auth/signup` |
| 로그인 | POST | `/api/auth/login` |
| 내 정보 조회 | GET | `/api/members/me` |
| 내 정보 수정 | PATCH | `/api/members/me` |

---

## 5.2 상품

| 기능 | Method | URL |
|---|---|---|
| 상품 등록 | POST | `/api/products` |
| 상품 목록 조회 | GET | `/api/products` |
| 상품 상세 조회 | GET | `/api/products/{productId}` |
| 상품 수정 | PATCH | `/api/products/{productId}` |
| 상품 삭제 | DELETE | `/api/products/{productId}` |
| 상품 상태 변경 | PATCH | `/api/products/{productId}/status` |

상품 이미지는 `ImageUploadController`로 먼저 업로드한 뒤 반환받은 `imageKey` 목록을 상품 등록·수정 요청의 `imageKeys` 필드로 전달해 관리한다.
상품 도메인은 `products/{uuid}.{jpg|jpeg|png|webp}` 형식의 key만 저장한다.

---

## 5.3 찜

| 기능 | Method | URL |
|---|---|---|
| 관심상품 등록 | POST | `/api/products/{productId}/wishes` |
| 관심상품 취소 | DELETE | `/api/products/{productId}/wishes` |
| 내 관심상품 목록 | GET | `/api/members/me/wishes` |

---

## 5.4 검색/인기검색어

| 기능 | Method | URL | 설명 |
|---|---|---|---|
| 상품 검색 v1 | GET | `/api/v1/products/search` | 캐시 미적용 |
| 상품 검색 v2 | GET | `/api/v2/products/search` | Caffeine Local Cache 적용 |
| 인기 검색어 조회 | GET | `/api/search/popular` | Redis ZSet 기반 추천 |
| 최근 검색어 조회 | GET | `/api/search/recent` | 인증 사용자 |
| 최근 검색어 삭제 | DELETE | `/api/search/recent/{searchLogId}` | 본인 기록만 삭제 |

검색 파라미터 예시:

```text
keyword, category, status, page, size, sort
```

정렬 기준:

| sort 값 | 설명 |
|---|---|
| `LATEST` | 최신 등록순 |
| `OLDEST` | 오래된 등록순 |
| `PRICE_DESC` | 가격 높은순 |
| `PRICE_ASC` | 가격 낮은순 |

---

## 5.5 쿠폰

| 기능 | Method | URL |
|---|---|---|
| 쿠폰 목록 조회 | GET | `/api/coupons` |
| 쿠폰 발급 | POST | `/api/coupons/{couponId}/issue` |
| 내 쿠폰 목록 | GET | `/api/members/me/coupons` |
| 쿠폰 사용 처리 | PATCH | `/api/user-coupons/{userCouponId}/use` |

---

## 5.6 채팅

| 기능 | Method | URL |
|---|---|---|
| 거래 채팅방 생성/입장 | POST | `/api/chat/rooms/trade` |
| CS 채팅방 생성 | POST | `/api/chat/rooms/cs` |
| 내 채팅방 목록 | GET | `/api/chat/rooms` |
| 채팅방 상세 조회 | GET | `/api/chat/rooms/{roomId}` |
| 메시지 내역 조회 | GET | `/api/chat/rooms/{roomId}/messages` |
| 메시지 읽음 처리 | PATCH | `/api/chat/rooms/{roomId}/read` |
| CS 상태 변경 | PATCH | `/api/admin/chat/rooms/{roomId}/cs-status` |

STOMP destination:

```text
CONNECT: Authorization: Bearer {accessToken}
SEND: /pub/chat/rooms/{roomId}/messages
SUBSCRIBE: /sub/chat/rooms/{roomId}
```

---

## 6. 구현 시 우선순위

## 6.1 먼저 확정해야 할 것

1. 공통 응답 구조 `ApiResponse`
2. 공통 예외 구조 `GlobalExceptionHandler`
3. 인증/인가 구조 `Spring Security + JWT`
4. `BaseEntity`
5. Enum 패키지 위치
6. ERD 기반 Entity 생성
7. Repository 기본 메서드와 유니크 제약조건

## 6.2 기능 구현 순서 추천

```text
1. members/auth
2. products/product_images
3. wishes
4. search/search_logs
5. coupons/user_coupons + Redis Lock
6. chat_rooms/chat_members/chat_messages
7. WebSocket/STOMP 인증
8. Redis Pub/Sub
9. 배포/CI-CD
```

---

## 7. Codex 작업 규칙

Codex는 코드를 생성할 때 다음 규칙을 지킨다.

1. ERD에 없는 테이블을 임의로 추가하지 않는다.
2. `coupons.issued_qty`를 사용해 현재 발급 수량을 관리한다.
3. 구매 요청, 주문, 결제 테이블은 생성하지 않는다.
4. 거래 의사는 채팅으로만 처리한다.
5. 상품 거래 상태는 `ProductStatus`로 관리한다.
6. 채팅 메시지는 `ChatMessage -> ChatRoom` 단방향 중심으로 설계한다.
7. 클라이언트가 보낸 `memberId`, `senderId`는 신뢰하지 않는다.
8. 인증된 사용자 정보는 SecurityContext 또는 STOMP Principal에서 가져온다.
9. 모든 목록 조회는 페이징을 적용한다.
10. 채팅 메시지 조회는 offset 페이징이 아니라 cursor 페이징을 우선한다.
11. Redis Lock은 쿠폰 발급에만 우선 적용한다.
12. 캐시는 검색 API v2부터 적용한다.
13. 관리자 API는 `/api/admin/**` 경로를 사용한다.
14. 삭제는 가능한 한 소프트 삭제를 우선한다.

---

## 8. 현재 ERD에서 보완하면 좋은 점

### 8.1 반드시 반영 권장

| 항목 | 이유 |
|---|---|
| `coupons.issued_qty` | 선착순 수량 검증과 동시성 테스트에 사용 |
| `members.email` unique | 로그인 식별자 중복 방지 |
| `wishes(member_id, product_id)` unique | 중복 찜 방지 |
| `user_coupons(member_id, coupon_id)` unique | 쿠폰 중복 발급 방지 |
| `chat_members(chat_room_id, member_id)` unique | 채팅방 중복 참여 방지 |
| `chat_messages(chat_room_id, id)` index | 커서 기반 메시지 조회 성능 확보 |

### 8.2 선택 반영

| 항목 | 판단 |
|---|---|
| 거래 채팅방 buyer/seller 컬럼 추가 | 중복 거래 채팅방 방지를 DB에서 강하게 막고 싶으면 추가 |
| `products.view_count` | 인기 상품 기능을 할 때만 추가 |
| `chat_rooms.last_message_id` | 채팅방 목록에서 최근 메시지 미리보기가 필요하면 추가 |
| `chat_rooms.deleted_at` | 채팅방 나가기/숨김 정책을 세밀하게 할 때 추가 |

현재 단계에서는 과한 확장은 피한다. 핵심은 검색 캐싱, 쿠폰 동시성, 채팅, 배포 산출물을 완성하는 것이다.

---

## 9. 발표/README에 남길 기술 포인트

### 검색/캐싱

- 왜 검색 API v1/v2를 분리했는가
- Local Cache의 장점과 Scale-out 한계
- Redis Cache로 전환할 경우 장점
- 인기 검색어를 Redis ZSet으로 관리하는 이유

### 쿠폰/동시성

- 동시에 여러 명이 쿠폰 발급 요청 시 초과 발급이 발생하는 이유
- 실패하는 동시성 테스트를 먼저 작성한 이유
- Redis Lock을 선택한 이유
- Lock key, TTL, UUID, Lua Script 해제 전략

### 채팅

- WebSocket을 사용하는 이유
- STOMP를 사용하는 이유
- HTTP Filter가 아니라 ChannelInterceptor에서 JWT를 검증하는 이유
- ChatMessage가 ChatRoom을 참조하는 단방향 설계 이유
- 커서 기반 페이징을 사용하는 이유

### 배포/CI-CD

- Docker로 실행 환경을 표준화한 이유
- GitHub Actions에서 테스트 실패 시 배포를 막는 이유
- AWS에서 민감 정보를 분리 관리하는 이유

---

## 10. 최종 결론

이 ERD는 부트캠프 팀프로젝트 기준으로 충분히 구현 가능한 구조다. 다만 실무적으로 중요한 것은 테이블을 더 늘리는 것이 아니라, 현재 ERD 위에서 다음을 명확하게 증명하는 것이다.

1. 검색 성능 개선 전/후 비교
2. 쿠폰 동시성 문제 재현 및 해결
3. 채팅 메시지 저장/조회/실시간 전달
4. JWT 기반 인증/인가
5. Docker 및 CI/CD 배포 자동화

Codex는 위 기준을 벗어나 과도한 아키텍처나 불필요한 도메인을 생성하지 말고, 현재 ERD와 팀 역할에 맞춰 구현한다.
