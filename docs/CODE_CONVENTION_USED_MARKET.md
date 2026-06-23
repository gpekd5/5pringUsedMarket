# 코딩 컨벤션

> Spring Boot 기반 중고거래 커머스 백엔드 프로젝트의 코드 작성 규칙이다.
>
> 엄격한 DDD 구조보다 **기능 단위 패키지 + 역할별 하위 패키지**를 따른다.
>
> 목표는 빠른 구현이 아니라, 팀원이 서로의 코드를 쉽게 읽고 수정할 수 있는 구조를 만드는 것이다.

---

## 1. 기본 원칙

- 기능 단위로 패키지를 나눈다.
- 각 기능 내부는 역할에 따라 `controller`, `service`, `repository`, `dto`, `entity`로 구분한다.
- Controller는 요청과 응답만 담당한다.
- Service는 비즈니스 로직과 트랜잭션을 담당한다.
- Repository는 DB 접근만 담당한다.
- DTO는 요청/응답 데이터를 전달하는 용도로만 사용한다.
- Entity는 DB 테이블과 매핑되는 객체이며, 핵심 상태 변경 메서드를 포함할 수 있다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 인증 사용자의 `memberId`, `senderId`는 클라이언트 요청값이 아니라 인증 객체에서 꺼낸다.
- Redis는 DB 테이블이 아니므로 ERD에 포함하지 않고, 아키텍처 문서에 표현한다.

---

## 1.1 주석 작성 원칙

팀원 간 코드 리뷰와 학습 효율을 위해 설명이 필요한 클래스와 메서드에는 주석을 작성한다.

- 클래스 설명은 클래스 선언 바로 위에 JavaDoc 형식으로 작성한다.
- 메서드 내부의 짧은 보조 설명은 `//` 한 줄 주석을 사용한다.
- 단순히 코드 내용을 반복하는 주석은 작성하지 않는다.
- 도메인 규칙, 보안 의도, 확장 포인트처럼 코드를 처음 보는 팀원이 이해해야 하는 맥락을 우선 설명한다.

```java
/**
 * 회원 정보를 저장하는 JPA 엔티티다.
 * 이메일과 닉네임은 서비스 내에서 유일해야 한다.
 */
public class Member {

    public static Member create(String email, String encodedPassword, String nickname) {
        // 신규 가입 회원은 기본 권한을 MEMBER로 생성한다.
        return new Member(email, encodedPassword, nickname, MemberRole.MEMBER);
    }
}
```

---

## 2. 패키지 구조

기능 단위 패키지 아래에 역할별 패키지를 둔다.

```text
com.example.fivespringusedmarket
├── common
│   ├── config
│   ├── entity
│   ├── exception
│   ├── response
│   └── security
│
├── auth
│   ├── controller
│   ├── service
│   └── dto
│
├── member
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── product
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── search
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── wish
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── chat
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── mypage
│   ├── controller
│   ├── service
│   └── dto
│
└── coupon
    ├── controller
    ├── service
    ├── repository
    ├── dto
    └── entity
```

### 현재 프로젝트 기준

- `presentation`, `application`, `domain`, `infrastructure` 같은 계층형 DDD 패키지는 사용하지 않는다.
- 부트캠프 팀 프로젝트에서는 역할 중심 구조가 더 빠르고 협업하기 쉽다.
- 단, 책임 분리는 반드시 지킨다.

---

## 3. 역할별 책임

| 패키지 | 책임 |
| --- | --- |
| `controller` | HTTP 요청/응답 처리, Request DTO 검증, Service 호출 |
| `service` | 비즈니스 로직 처리, 트랜잭션 관리, Entity 상태 변경 |
| `repository` | DB 조회/저장/수정/삭제 |
| `dto` | Request, Response, 내부 전달용 DTO |
| `entity` | JPA Entity, DB 테이블 매핑, 상태 변경 메서드 |
| `common` | 공통 응답, 예외, 보안, Redis, 설정 |

---

## 4. 의존 방향

기본 흐름은 아래 방향을 따른다.

```text
Controller → Service → Repository → Entity
```

각 역할은 아래 원칙을 지킨다.

- Controller는 Service만 호출한다.
- Controller에서 Repository를 직접 호출하지 않는다.
- Controller에서 비즈니스 로직을 작성하지 않는다.
- Service는 Repository를 통해 Entity를 조회하거나 저장한다.
- Repository는 DB 접근만 담당한다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- 다른 도메인의 Repository를 직접 많이 끌어오는 구조는 피하고, 필요하면 해당 도메인 Service 메서드로 위임한다.

---

## 5. DTO 규칙

DTO는 각 도메인의 `dto` 패키지에 둔다.

```text
product
└── dto
    ├── ProductCreateRequest
    ├── ProductUpdateRequest
    ├── ProductSearchRequest
    ├── ProductResponse
    ├── ProductDetailResponse
    └── ProductStatusUpdateRequest
```

### Request DTO

- 클라이언트 요청 데이터를 받는 용도이다.
- Controller에서 사용한다.
- 검증 어노테이션을 사용한다.
- 인증된 회원 ID, 발신자 ID는 Request DTO에 넣지 않는다.

```java
public record ProductCreateRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull Integer price,
        @NotBlank String category
) {
}
```

### Response DTO

- 클라이언트에게 반환할 데이터를 담는다.
- Entity를 직접 반환하지 않고 Response DTO로 변환한다.
- 단순 조회는 `from(entity)` 정적 팩토리를 사용한다.
- 여러 Entity 조합 응답은 Service에서 필요한 데이터를 조립한다.

```java
public record ProductResponse(
        Long productId,
        String title,
        Integer price,
        String status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStatus().name()
        );
    }
}
```

---

## 6. Controller 규칙

- 요청을 받고 응답을 반환하는 역할만 한다.
- Request DTO를 검증한다.
- Service를 호출한다.
- Entity를 직접 반환하지 않는다.
- Repository를 직접 호출하지 않는다.
- 인증 사용자는 `@AuthenticationPrincipal` 또는 공통 인증 객체로 받는다.
- HTTP Status와 내부 ErrorCode를 혼동하지 않는다.

좋은 예시:

```java
@PostMapping("/api/products")
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @AuthenticationPrincipal AuthMember authMember,
        @Valid @RequestBody ProductCreateRequest request
) {
    ProductResponse response = productService.createProduct(authMember.id(), request);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

피해야 할 예시:

```java
@PostMapping("/api/products")
public Product createProduct(@RequestBody ProductCreateRequest request) {
    return productRepository.save(new Product(request.title(), request.description()));
}
```

---

## 7. Service 규칙

- 비즈니스 로직은 Service에서 처리한다.
- 트랜잭션은 Service 메서드에 적용한다.
- Entity 조회, 검증, 상태 변경, 저장 흐름을 담당한다.
- Controller로부터 받은 Request DTO를 사용할 수 있다.
- Service에서 Response DTO로 변환하여 Controller에 반환한다.
- 외부 시스템 호출, Redis Lock, 캐시 무효화는 트랜잭션 범위와 충돌하지 않도록 주의한다.

```java
@Transactional
public ProductResponse createProduct(Long memberId, ProductCreateRequest request) {
    Member seller = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

    Product product = Product.create(
            seller,
            request.title(),
            request.description(),
            request.price(),
            ProductCategory.valueOf(request.category())
    );

    Product savedProduct = productRepository.save(product);

    return ProductResponse.from(savedProduct);
}
```

### 트랜잭션 기준

- 조회 전용 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- 생성, 수정, 삭제, 상태 변경은 `@Transactional`을 사용한다.
- Redis Lock을 사용하는 쿠폰 발급은 Lock 획득 후 트랜잭션을 시작하는 구조를 우선 고려한다.
- 트랜잭션 안에서 불필요한 외부 API 호출, 긴 대기, Thread sleep을 하지 않는다.

---

## 8. Repository 규칙

- Repository는 DB 접근만 담당한다.
- Repository에서 비즈니스 로직을 처리하지 않는다.
- 단순 CRUD는 Spring Data JPA Repository를 사용한다.
- 복잡한 조건 검색은 `@Query` 또는 QueryDSL을 사용할 수 있다.
- 검색 API는 페이징을 기본으로 한다.
- 대량 메시지 조회는 offset보다 cursor 기반 조회를 우선한다.

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByTitleContainingAndStatusNot(
            String keyword,
            ProductStatus status,
            Pageable pageable
    );
}
```

채팅 메시지 커서 조회 예시:

```java
@Query("""
    SELECT m
    FROM ChatMessage m
    JOIN FETCH m.sender
    WHERE m.chatRoom.id = :roomId
      AND (:lastMessageId IS NULL OR m.id < :lastMessageId)
    ORDER BY m.id DESC
""")
List<ChatMessage> findMessagesByCursor(
        @Param("roomId") Long roomId,
        @Param("lastMessageId") Long lastMessageId,
        Pageable pageable
);
```

---

## 9. Entity 규칙

- Entity는 `entity` 패키지에 둔다.
- Entity는 JPA 매핑 정보를 가진다.
- setter를 무분별하게 열지 않는다.
- 상태 변경은 의미 있는 메서드로 처리한다.
- 생성자는 protected로 제한하고, 생성은 정적 팩토리 메서드를 우선 사용한다.

좋은 예시:

```java
public void reserve() {
    if (this.status != ProductStatus.ON_SALE) {
        throw new CustomException(ErrorCode.INVALID_PRODUCT_STATUS);
    }
    this.status = ProductStatus.RESERVED;
}

public void completeSale() {
    if (this.status == ProductStatus.DELETED) {
        throw new CustomException(ErrorCode.DELETED_PRODUCT);
    }
    this.status = ProductStatus.SOLD;
}
```

피해야 할 예시:

```java
product.setStatus(ProductStatus.SOLD);
product.setTitle(title);
product.setPrice(price);
```

---

## 10. 공통 응답 규칙

모든 HTTP 응답은 `ApiResponse<T>`를 사용한다.

### 성공 응답

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 실패 응답

```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "에러 메시지"
}
```

### 규칙

- 성공 응답은 `ApiResponse.success(data)`를 사용한다.
- 실패 응답은 `GlobalExceptionHandler`에서 일괄 처리한다.
- Controller에서 try-catch로 예외를 반복 처리하지 않는다.
- HTTP Status와 내부 ErrorCode는 분리한다.

```text
HTTP Status: 401 Unauthorized
Error Code: BLACKLIST_TOKEN
```

---

## 11. 예외 처리 규칙

- 비즈니스 예외는 `CustomException`을 사용한다.
- 에러 코드는 `ErrorCode` enum으로 관리한다.
- 공통 예외 처리는 `GlobalExceptionHandler`에서 담당한다.
- 인증 실패는 Security Filter 또는 `AuthenticationEntryPoint`에서 처리한다.
- 권한 부족은 `AccessDeniedHandler`에서 처리한다.

```java
throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
```

### ErrorCode 네이밍 예시

```java
MEMBER_NOT_FOUND
DUPLICATED_EMAIL
INVALID_PASSWORD
PRODUCT_NOT_FOUND
PRODUCT_OWNER_MISMATCH
INVALID_PRODUCT_STATUS
COUPON_NOT_FOUND
COUPON_ALREADY_ISSUED
COUPON_OUT_OF_STOCK
CHAT_ROOM_NOT_FOUND
CHAT_ACCESS_DENIED
BLACKLIST_TOKEN
EXPIRED_TOKEN
```

---

## 12. 인증 / 인가 규칙

- JWT 인증은 Security Filter에서 처리한다.
- 인증된 사용자 정보는 `SecurityContextHolder`에 저장한다.
- Controller에서는 `@AuthenticationPrincipal AuthMember`로 사용자 정보를 받는다.
- 관리자 권한은 Controller 내부 if문보다 Security 설정에서 우선 처리한다.
- 클라이언트가 보낸 `memberId`, `senderId`, `sellerId`를 신뢰하지 않는다.

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{productId}").permitAll()
.requestMatchers(HttpMethod.GET, "/api/members/{memberId}/profile").permitAll()
.requestMatchers(HttpMethod.GET, "/api/coupons").permitAll()
.requestMatchers(HttpMethod.GET, "/api/search/popular").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/products/search", "/api/v2/products/search").permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

### JWT / Redis 규칙

- Refresh Token은 Redis에 저장한다.
- 로그아웃 시 Access Token을 Redis Blacklist에 저장한다.
- Blacklist TTL은 Access Token의 남은 만료 시간으로 설정한다.
- Refresh Token 삭제로 토큰 재발급을 차단한다.

Redis Key 예시:

```text
refresh-token:{memberId}
blacklist:access-token:{token}
```

---

## 13. 상품 도메인 규칙

상품 관련 클래스는 `product` 패키지 아래에 둔다.

### 상품 상태

```java
public enum ProductStatus {
    ON_SALE,
    RESERVED,
    SOLD,
    DELETED
}
```

### 규칙

- 상품 삭제는 소프트 삭제로 처리한다.
- 삭제 상태는 `DELETED`를 사용한다.
- 본인 상품만 수정, 삭제, 상태 변경 가능하다.
- SOLD 상태 상품은 수정 불가하다.
- 판매자는 상품 상태를 `ON_SALE`, `RESERVED`, `SOLD`로 변경할 수 있다.
- 구매 요청 테이블을 별도로 두지 않고, 구매 의사는 채팅으로 협의한다.
- 상품 이미지는 필요하면 `sortOrder`로 노출 순서를 관리한다.

### 대표 도메인 메서드

```java
public void update(String title, String description, Integer price, ProductCategory category)
public void reserve()
public void cancelReservation()
public void completeSale()
public void delete()
```

---

## 14. 관심상품 규칙

- 관심상품 도메인은 `wish` 패키지를 사용한다.
- 한 회원은 같은 상품을 한 번만 관심 등록할 수 있다.
- DB에 `member_id + product_id` Unique 제약을 둔다.
- 본인 상품도 관심 등록할 수 있는지 여부는 정책으로 명확히 정한다.

```text
UNIQUE(member_id, product_id)
```

---

## 15. 쿠폰 / 동시성 규칙

선착순 쿠폰은 발제 요구사항의 동시성 제어 핵심 기능이다.

### 규칙

- 쿠폰 관련 클래스는 `coupon` 패키지 아래에 둔다.
- 선착순 쿠폰 발급은 Redis Lock으로 보호한다.
- 쿠폰 발급 조건은 이벤트 기간, 잔여 수량, 중복 발급 여부다.
- 동일 회원은 동일 쿠폰을 중복 발급받을 수 없다.
- `user_coupon`에는 `member_id + coupon_id` Unique 제약을 둔다.
- 쿠폰 수량 차감과 UserCoupon 저장은 하나의 트랜잭션에서 처리한다.

```text
UNIQUE(member_id, coupon_id)
```

### Redis Lock Key

```text
lock:coupon:{couponId}
```

### Redis Lock 구현 원칙

- `SETNX + TTL` 기반으로 구현한다.
- Lock 획득 실패 전략은 Fail Fast 또는 짧은 Retry 중 하나로 통일한다.
- Lock 해제 시 본인이 획득한 Lock만 해제한다.
- UUID 값 비교 후 Lua Script로 원자적 삭제를 우선 고려한다.
- Lock 범위 안에서 불필요한 긴 로직을 수행하지 않는다.

---

## 16. 검색 / 캐싱 규칙

검색 관련 클래스는 `search` 패키지 아래에 둔다.

### 검색 API

- v1 검색 API는 캐시를 적용하지 않는다.
- v2 검색 API는 Caffeine Local Cache를 적용한다.
- 검색은 `LIKE` 조건을 사용한다.
- 검색 결과는 페이지네이션한다.
- 로그인 사용자가 keyword 검색을 하면 최근 검색어를 저장한다.
- 인기검색어는 Redis ZSet 사용을 우선 고려한다.

예시 URL:

```text
GET /api/v1/products/search
GET /api/v2/products/search
GET /api/search/popular
GET /api/search/recent
```

### 캐시 규칙

- 캐시 Key는 검색 조건과 페이지 조건을 포함한다.
- Key 충돌을 막기 위해 prefix를 사용한다.
- TTL과 maximumSize를 설정한다.
- 상품 수정, 삭제, 상태 변경 시 검색 캐시 무효화를 고려한다.

```java
@Cacheable(
        value = "productSearch",
        key = "'keyword:' + #request.keyword() + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize"
)
public Page<ProductResponse> searchProductsV2(ProductSearchRequest request, Pageable pageable) {
    // search logic
}
```

### README 기록 필수

- 왜 검색 API에 캐시를 적용했는가
- v1과 v2의 응답 시간 차이
- Local Cache의 한계
- Redis Cache로 확장할 경우의 장점
- 캐시 무효화 전략

---

## 17. WebSocket / 채팅 규칙

채팅 관련 클래스는 `chat` 패키지 아래에 둔다.

### 기본 규칙

- 거래 채팅과 CS 문의 채팅을 모두 `chat` 도메인에서 관리한다.
- 채팅 메시지는 DB에 저장한다.
- `ChatMessage`가 `ChatRoom`을 단방향 참조하는 구조를 우선한다.
- 메시지 조회는 커서 기반 페이징을 사용한다.
- 메시지 전송 시 `senderId`는 클라이언트가 보내지 않는다.
- 서버가 `Principal`에서 발신자를 식별한다.

### STOMP 인증 규칙

- STOMP CONNECT 시점에 `ChannelInterceptor`에서 JWT를 검증한다.
- 검증된 사용자 정보를 `Principal`로 등록한다.
- `@MessageMapping`에서는 `Principal`로 발신자를 식별한다.

### Destination 규칙

```text
구독: /sub/chat/rooms/{roomId}
송신: /pub/chat/rooms/{roomId}/messages
```

### CS 문의 상태

```java
public enum CsStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED
}
```

### Redis Pub/Sub

- 다중 서버 확장 시 Redis Pub/Sub을 사용한다.
- 채팅방 단위로 Redis 채널을 분리한다.

```text
chat-room:{roomId}
```

---

## 18. 마이페이지 규칙

마이페이지는 화면 이동 API가 아니다.

`GET /api/mypage`는 마이페이지 첫 화면에 필요한 요약 정보만 반환한다.

```json
{
  "memberId": 1,
  "nickname": "현승",
  "sellingProductCount": 3,
  "wishedProductCount": 5,
  "chatRoomCount": 2,
  "couponCount": 1
}
```

### 도메인 소유권

URL에 `/members/me`가 들어간다고 해서 무조건 MemberController에 구현하지 않는다.

| API | 담당 도메인 |
| --- | --- |
| `/api/members/me` | Member |
| `/api/members/me/wishes` | Wish |
| `/api/members/me/coupons` | Coupon |
| `/api/products/me` | Product |
| `/api/chat/rooms` | Chat |
| `/api/mypage` | MyPage |

---

## 19. Redis 사용 규칙

Redis는 ERD에 포함하지 않는다.

Redis 사용처는 아래로 제한한다.

- Refresh Token 저장
- Access Token Blacklist
- 선착순 쿠폰 Redis Lock
- 인기검색어 ZSet
- 채팅 Redis Pub/Sub

검색 결과용 Redis Remote Cache는 현재 프로젝트 범위에 포함하지 않는다.

### Key 네이밍

```text
refresh-token:{memberId}
blacklist:access-token:{token}
lock:coupon:{couponId}
popular-keywords:{date}
chat-room:{roomId}
```

---

## 20. 네이밍 규칙

### Controller

```text
AuthController
MemberController
ProductController
WishController
SearchController
CouponController
ChatController
MyPageController
```

### Service

```text
AuthService
MemberService
ProductService
WishService
SearchService
CouponService
ChatService
MyPageService
```

### Repository

```text
MemberRepository
ProductRepository
ProductImageRepository
WishRepository
SearchKeywordRepository
CouponRepository
UserCouponRepository
ChatRoomRepository
ChatMessageRepository
```

### DTO

```text
SignupRequest
LoginRequest
TokenReissueRequest
LoginResponse
MemberResponse
ProductCreateRequest
ProductUpdateRequest
ProductSearchRequest
ProductResponse
ProductDetailResponse
CouponIssueRequest
CouponResponse
ChatRoomResponse
ChatMessageRequest
ChatMessageResponse
MyPageResponse
```

### Entity

```text
Member
Product
ProductImage
Wish
SearchKeyword
Coupon
UserCoupon
ChatRoom
ChatMessage
```

### Enum

```text
MemberRole
ProductStatus
ProductCategory
CouponStatus
ChatRoomType
CsStatus
```

---

## 21. 테스트 규칙

- Service 핵심 비즈니스 로직은 테스트를 작성한다.
- 쿠폰 동시성 제어는 반드시 동시 요청 테스트를 작성한다.
- 동시성 해결 전 실패 테스트와 해결 후 성공 테스트를 기록한다.
- 인증/인가, 상품 상태 변경, 쿠폰 중복 발급, 채팅방 중복 생성은 우선 테스트 대상이다.

동시성 테스트 키워드:

```text
ExecutorService
CountDownLatch
CyclicBarrier
Redis Lock
```

---

## 22. 배포 / CI/CD 규칙

최소 배포 구성은 아래를 목표로 한다.

```text
Dockerfile
docker-compose.yml
GitHub Actions CI
EC2 배포
```

### 로컬 개발 환경

- `docker-compose.yml`로 MySQL과 Redis를 함께 실행한다.
- `.env` 파일은 Git에 올리지 않는다.

### CI

- PR 또는 push 시 build/test를 수행한다.
- 테스트 실패 시 배포하지 않는다.
- main 직접 push를 금지한다.

### 민감 정보

- 로컬: `.env`
- GitHub Actions: GitHub Secrets
- 운영: 환경변수 또는 AWS Parameter Store

---

## 23. Git / 협업 규칙

### 브랜치 전략

```text
main      : 배포용
develop   : 개발 통합용
feature/* : 기능 개발용
```

예시:

```text
feature/auth-login
feature/product-create
feature/search-cache
feature/coupon-redis-lock
feature/chat-websocket
feature/infra-cicd
```

### PR 규칙

- main 직접 push 금지
- develop 직접 push 금지
- 기능 단위 PR 생성
- 최소 1명 이상 리뷰
- PR 본문에 구현 내용 작성
- 테스트 결과 작성
- AI 사용 여부와 검증 내용을 작성

### 커밋 메시지 예시

```text
feat: 로그인 API 구현
feat: 선착순 쿠폰 발급 API 구현
feat: 상품 검색 v2 캐시 적용
feat: WebSocket 채팅 메시지 전송 구현
fix: JWT 필터 예외 처리 수정
test: 쿠폰 발급 동시성 테스트 추가
docs: Redis Lock 선택 근거 작성
chore: Docker 설정 추가
```

---

## 24. AI 활용 규칙

- AI가 생성한 코드는 반드시 본인이 설명할 수 있어야 한다.
- AI 사용 내용은 PR 또는 기록 보드에 남긴다.
- AI에게 맡길 수 있는 작업은 초안 작성, 설계 검토, 테스트 케이스 후보 정리, 문서화다.
- AI가 만든 코드를 그대로 붙이지 말고 프로젝트 컨벤션에 맞게 수정한다.
- 이해하지 못한 코드는 머지하지 않는다.

---

## 25. 금지 사항

- Controller에서 Repository 직접 호출 금지
- Controller에서 비즈니스 로직 작성 금지
- Entity를 API 응답으로 직접 반환 금지
- DTO를 Entity처럼 사용 금지
- Repository에서 비즈니스 로직 처리 금지
- setter 무분별한 사용 금지
- 인증/인가 로직을 여러 Controller에 반복 작성 금지
- 예외 처리를 Controller마다 try-catch로 반복 작성 금지
- 인증 사용자의 memberId를 Request Body에서 신뢰 금지
- 채팅 senderId를 클라이언트 요청값으로 신뢰 금지
- Redis를 ERD 테이블처럼 표현 금지
- 존재하지 않는 ERD 필드를 API 응답에 임의 추가 금지
- PR 없이 main 또는 develop 직접 머지 금지
- 과도한 아키텍처 추가 금지

---

## 26. 현재 프로젝트 기준 정리

현재 중고거래 팀 프로젝트에서는 아래 구조를 기준으로 한다.

```text
기능 패키지
├── controller
├── service
├── repository
├── dto
└── entity
```

핵심은 아래다.

- Controller는 얇게 유지한다.
- Service에 비즈니스 흐름을 둔다.
- Repository는 DB 접근만 담당한다.
- Entity는 외부로 직접 노출하지 않는다.
- Response DTO로 변환해서 응답한다.
- 상품 거래는 구매 요청 테이블보다 채팅 협의와 상품 상태 변경 중심으로 간다.
- 필수 평가 포인트인 검색 캐싱, 쿠폰 Redis Lock, 동시성 테스트는 README에 반드시 근거를 남긴다.
- 채팅은 STOMP 인증, 메시지 저장, 커서 기반 조회를 우선 구현한다.
