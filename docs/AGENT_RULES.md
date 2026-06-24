# AGENT_RULES.md

## 0. 문서 기준과 우선순위

이 저장소의 Markdown 문서는 사람과 AI가 함께 사용하는 프로젝트 컨텍스트다.

문서가 충돌할 경우 아래 순서로 판단한다.

1. `API_SPEC.md`: HTTP 요청·응답과 URL 계약
2. `ERD_GUIDE_FOR_CODEX.md`: 테이블, 컬럼, 관계, 제약조건
3. `CODE_CONVENTION_USED_MARKET.md`: 패키지 구조와 코드 책임
4. `SECURITY_ARCHITECTURE.md`: 인증·인가와 Redis 토큰 정책
5. `DOMAIN_OWNERSHIP.md`: 담당 영역과 협업 경계
6. `PROJECT_CONTEXT.md`: 프로젝트 목표와 발제 원문

충돌 또는 새로운 정책이 발견되면 코드에서 임의로 결정하지 않고 관련 문서를 먼저 수정한다.

현재 확정된 공통 용어는 다음과 같다.

- 루트 패키지: `com.example.fivespringusedmarket`
- 상품 삭제 상태: `DELETED`
- 관심상품 도메인 및 URL: `wish`, `/wishes`
- 관심상품 여부 응답 필드: `wished`
- 상품 설명 필드: `description`
- CS 상태 Enum: `CsStatus`
- 쿠폰 Lock Key: `lock:coupon:{couponId}`
- 검색 v2 캐시: Caffeine Local Cache

## 1. 프로젝트 기본 원칙

이 프로젝트는 Spring Boot 기반 중고거래 커머스 백엔드 프로젝트다.

주요 기능은 다음과 같다.

- 인증 / 회원
- 상품 등록 / 조회 / 수정 / 삭제
- 상품 상태 관리
- 관심상품
- 검색 / 인기검색어 / 최근검색어
- 선착순 이벤트 쿠폰
- 실시간 채팅
- 마이페이지 요약 조회
- Docker / GitHub Actions / EC2 배포

발제 요구사항상 반드시 신경 써야 하는 기술 미션은 다음이다.

- 검색 API + 인기검색어
- 검색 API Local Cache 적용
- 선착순 이벤트 쿠폰
- Redis Lock을 활용한 동시성 제어
- 동시성 테스트 코드
- README에 기술 선택 이유와 성능 비교 기록

## 2. 코드 작성 원칙

- Controller는 요청/응답만 담당한다.
- Service는 비즈니스 로직을 담당한다.
- Repository는 DB 접근만 담당한다.
- Entity를 Controller 응답으로 직접 반환하지 않는다.
- Request / Response DTO를 분리한다.
- 공통 응답은 `ApiResponse<T>`를 사용한다.
- 예외는 `CustomException` + `ErrorCode` + `GlobalExceptionHandler` 구조를 사용한다.
- 인증 사용자는 `@AuthenticationPrincipal` 또는 공통 Auth 객체에서 꺼낸다.
- 클라이언트가 memberId, senderId 같은 인증 관련 식별자를 Body로 보내더라도 신뢰하지 않는다.
- 테스트 코드는 `given`, `when`, `then` 순서로 구분해서 작성한다.

## 3. 패키지 구조 예시

```text
com.example.fivespringusedmarket
 ├─ common
 │   ├─ config
 │   ├─ entity
 │   ├─ exception
 │   ├─ response
 │   └─ security
 ├─ auth
 ├─ member
 ├─ product
 ├─ coupon
 ├─ wish
 ├─ search
 ├─ chat
 └─ mypage
```

각 기능 패키지 내부는 필요한 범위에서 `controller`, `service`, `repository`, `dto`, `entity`로 나눈다.

## 4. 인증 / JWT 규칙

- 로그인 성공 시 Access Token과 Refresh Token을 발급한다.
- Access Token 만료 시간은 30분으로 한다.
- Refresh Token 만료 시간은 14일로 한다.
- Access Token은 일반 API 요청에 사용한다.
- Refresh Token은 Access Token 재발급에 사용한다.
- Refresh Token은 Redis에 저장하고 Whitelist 방식으로 관리한다.
- 토큰 재발급 시 요청 Refresh Token과 Redis에 저장된 Refresh Token을 비교한다.
- 토큰 재발급 성공 시 Refresh Token Rotation 전략에 따라 새 Refresh Token으로 교체한다.
- 재발급에 사용된 기존 Refresh Token은 더 이상 사용할 수 없다.
- 로그아웃 시 Access Token을 Redis Blacklist에 저장한다.
- 로그아웃 시 Redis의 Refresh Token을 삭제한다.
- Blacklist TTL은 Access Token의 남은 만료 시간으로 설정한다.
- 만료된 Access Token은 Blacklist에 넣지 않는다.
- 인증 필터는 JWT 검증 후 Redis Blacklist 여부를 확인한다.

## 5. Redis 사용 규칙

Redis는 ERD에 포함하지 않는다. 시스템 아키텍처 문서에 표현한다.

Redis 사용처:

- Refresh Token 저장
- Access Token Blacklist
- 선착순 쿠폰 Redis Lock
- 인기검색어 ZSet
- 채팅 Redis Pub/Sub

검색 결과용 Redis Remote Cache는 현재 프로젝트 범위에 포함하지 않는다.

Redis 패키지 위치:

- Redis 연결, 직렬화, 공통 설정은 `common.config`에 둔다.
- 도메인별 Redis 접근 로직은 각 도메인의 `repository` 패키지에 둔다.
- Redis 사용 로직을 공통 패키지에 일괄 배치하지 않는다.
- 예: `auth.repository.RefreshTokenRedisRepository`, `coupon.repository.CouponRedisLockRepository`, `search.repository.PopularKeywordRedisRepository`, `chat.repository.ChatRedisPublisher`

Redis Lock 구현 시 고려사항:

- Lock Key는 비즈니스 기준으로 설계한다. 예: `lock:coupon:{couponId}`
- SETNX + TTL 기반으로 구현한다.
- Lock 획득 실패 시 Fail Fast 또는 짧은 Retry 중 하나를 선택한다.
- Lock 해제 시 본인이 획득한 Lock만 해제해야 한다.
- UUID + Lua Script 삭제를 우선 고려한다.
- Lock 범위 안에서 불필요하게 긴 로직을 수행하지 않는다.

## 6. API 응답 규칙

성공:

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

실패:

```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "에러 메시지"
}
```

HTTP Status와 내부 Error Code는 분리한다.

예:

```text
HTTP Status: 401 Unauthorized
Error Code: BLACKLIST_TOKEN
```

## 7. 도메인 소유권 규칙

URL에 `/members/me`가 들어간다고 해서 MemberController에 구현하지 않는다.

- `/api/members/me` → Member 담당
- `/api/members/me/wishes` → Wish 담당
- `/api/members/me/coupons` → Coupon 담당
- `/api/products/me` → Product 담당
- `/api/chat/rooms` → Chat 담당
- `/api/mypage` → MyPage 담당

도메인 기준으로 Controller와 Service를 배치한다.

## 8. 마이페이지 규칙

마이페이지는 화면 이동 API가 아니다.

`GET /api/mypage`는 마이페이지 첫 화면에 필요한 요약 정보만 반환한다.

예:

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

내 판매 상품 목록, 관심상품 목록, 채팅방 목록, 내 쿠폰 목록은 각각의 도메인 API에서 조회한다.

## 9. Product 규칙

- 상품 삭제는 우선 소프트 삭제로 처리한다.
- 상품 상태는 `ON_SALE`, `RESERVED`, `SOLD`, `DELETED`를 사용한다.
- SOLD 상태 상품은 수정 불가하다.
- 본인 상품만 수정, 삭제, 상태 변경 가능하다.
- 상품 상세 조회는 로그인 여부에 따라 `wished` 값을 계산할 수 있다.
- 상품 이미지 정렬이 필요하면 sortOrder를 사용한다.

## 10. Coupon 규칙

- 선착순 쿠폰 발급은 Redis Lock으로 보호한다.
- 쿠폰 발급 조건은 이벤트 기간, 잔여 수량, 중복 발급 여부다.
- 동일 회원은 동일 쿠폰을 중복 발급받을 수 없다.
- `user_coupons`에는 `member_id + coupon_id` Unique 제약을 둔다.
- 쿠폰 수량 차감과 UserCoupon 저장은 하나의 트랜잭션에서 처리한다.

## 11. Search 규칙

- v1 검색 API는 캐시가 없다.
- v2 검색 API는 Caffeine Local Cache를 적용한다.
- 검색은 LIKE 조건을 사용한다.
- 검색 결과는 페이지네이션한다.
- 로그인 사용자가 keyword 검색을 하면 최근 검색어를 저장한다.
- 인기검색어는 Redis ZSet 사용을 우선 고려한다.
- README에 캐시 적용 전/후 성능 비교를 기록한다.

## 12. Chat 규칙

- 채팅 메시지는 DB에 저장한다.
- ChatMessage가 ChatRoom을 단방향 참조하는 구조를 우선한다.
- 메시지 조회는 커서 기반 페이징을 사용한다.
- STOMP CONNECT 시점에 ChannelInterceptor에서 JWT를 검증한다.
- 메시지 전송 시 senderId는 클라이언트가 보내지 않는다.
- 서버가 Principal에서 senderId를 식별한다.
- 채팅방 destination은 `/sub/chat/rooms/{roomId}` 형식으로 분리한다.
- 메시지 송신 destination은 `/pub/chat/rooms/{roomId}/messages`를 사용한다.
- 다중 서버 확장 시 Redis Pub/Sub을 사용한다.

## 13. 배포 규칙

최소 배포 구성:

- Dockerfile
- docker-compose.yml
- GitHub Actions CI
- EC2 배포

우선순위:

1. Docker로 로컬 실행 가능하게 만들기
2. EC2에 Docker 설치 및 컨테이너 실행
3. GitHub Actions에서 build/test
4. Docker image build
5. EC2 배포 자동화

민감정보는 코드에 하드코딩하지 않는다.

- 로컬: `.env`
- GitHub Actions: GitHub Secrets
- 운영: 환경변수 또는 AWS Parameter Store

## 14. 금지사항

- PR 없이 main 직접 머지 금지
- Entity 직접 응답 금지
- Controller에 비즈니스 로직 작성 금지
- 인증 사용자의 memberId를 Request Body에서 신뢰 금지
- Redis를 ERD 테이블처럼 표현 금지
- 존재하지 않는 ERD 필드를 API 응답에 임의 추가 금지
- 과도한 아키텍처 추가 금지
