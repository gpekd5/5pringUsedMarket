# DOMAIN_OWNERSHIP.md

## 프로젝트 도메인 소유권

본 문서는 각 팀원의 책임 범위를 명확하게 정의하여
기능 중복 개발 및 책임 충돌을 방지하기 위한 문서이다.

---

# 김현승

## 담당 영역

### 인증 / 인가 (Authentication & Authorization)

- 회원가입
- 로그인
- 로그아웃
- 토큰 재발급
- JWT 인증
- JWT 인가
- Security 설정

### 공통(Common)

- ApiResponse
- ErrorCode
- CustomException
- GlobalExceptionHandler
- BaseEntity
- 공통 설정

### 마이페이지(MyPage)

- 마이페이지 요약 조회

### 배포(Infra)

- Docker
- Docker Compose
- GitHub Actions
- AWS EC2
- CI/CD
- 환경변수 관리
- 운영 환경 구성

---

# 황정후

## 담당 영역

### 동시성(Concurrency)

- 선착순 쿠폰 발급
- Redis Lock
- LockService
- LockRedisRepository
- 동시성 테스트 코드

### 쿠폰(Coupon)

- 이벤트 쿠폰 조회
- 쿠폰 발급
- 발급 이력 조회
- 중복 발급 방지
- 수량 검증

### Redis Lock 설계

- Lock Key 설계
- TTL 설정
- UUID 기반 Lock 해제
- Lua Script 기반 Lock 해제

---

# 김홍기

## 담당 영역

### 검색(Search)

- 상품 검색
- 최근 검색어
- 검색 로그

### 캐싱(Cache)

- 상품 검색 API v1
- 상품 검색 API v2
- Caffeine Cache
- Cache Eviction

### 인기 검색어

- Redis ZSet
- 인기 검색어 집계
- 인기 검색어 조회

### 성능 측정

- 캐시 적용 전 성능 측정
- 캐시 적용 후 성능 측정
- README 성능 비교 작성

---

# 윤영범

## 담당 영역

### 실시간 채팅(Chat)

- 거래 채팅
- CS 문의 채팅
- 채팅방 관리
- 메시지 저장

### WebSocket

- WebSocket 설정
- STOMP 설정
- 메시지 송수신

### Redis Pub/Sub

- Publisher
- Subscriber
- 다중 서버 채팅 처리

### 채팅 조회

- 채팅 내역 조회
- 커서 기반 페이징
- 읽음 처리

---

# 공통 규칙

- 각 팀원은 본인 도메인의 Controller, Service, Repository를 관리한다.
- 다른 도메인의 코드를 수정할 경우 담당자와 사전 협의한다.
- 공통 정책 변경은 팀 전체 논의 후 진행한다.
- Security 관련 변경은 김현승 검토 후 반영한다.
- Redis Lock 관련 변경은 황정후 검토 후 반영한다.
- Cache 관련 변경은 김홍기 검토 후 반영한다.
- Chat 관련 변경은 윤영범 검토 후 반영한다.
