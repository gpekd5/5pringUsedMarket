# README_DRAFT.md

## 중고거래 커머스 플랫폼

당근마켓과 유사한 중고거래 플랫폼 백엔드 프로젝트입니다.

구매 요청이나 결제 시스템 없이, 구매자와 판매자는 채팅으로 거래 의사를 협의합니다. 판매자는 상품 상태를 판매중, 예약중, 판매완료로 직접 변경합니다.

## 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- Spring Security
- JWT
- MySQL
- Redis
- WebSocket / STOMP
- Docker
- GitHub Actions
- AWS EC2

## 주요 기능

- 인증/회원
- 상품
- 검색/인기검색어
- 관심상품
- 선착순 쿠폰
- 실시간 채팅
- 마이페이지
- CI/CD 배포

## Redis 활용

### Refresh Token 저장

로그인 시 발급한 Refresh Token을 Redis에 저장합니다.  
토큰 재발급 요청 시 클라이언트가 전달한 Refresh Token과 Redis에 저장된 값을 비교하는 Whitelist 방식으로 관리합니다.

Refresh Token은 14일 동안 유효하며, 재발급 성공 시 기존 Refresh Token을 폐기하고 새로운 Refresh Token으로 교체하는 Rotation 전략을 적용합니다.  
이를 통해 Refresh Token 탈취 시 발생할 수 있는 재사용 공격 위험을 줄입니다.

### Access Token Blacklist

JWT는 Stateless 구조이기 때문에 로그아웃 후에도 Access Token이 만료 전까지 사용될 수 있습니다.  
이를 방지하기 위해 로그아웃 시 아직 만료되지 않은 Access Token을 Redis Blacklist에 저장하고, 인증 필터에서 Blacklist 여부를 확인합니다.

Access Token은 30분 동안 유효하며, Blacklist TTL은 Access Token의 남은 만료 시간으로 설정합니다.  
현재 서비스 특성상 Blacklist가 반드시 필요한 수준은 아니지만, 로그아웃 이후 Access Token 사용 가능 문제를 직접 해결하고 Redis 기반 인증 전략을 학습하기 위해 적용합니다.

### 선착순 쿠폰 Redis Lock

선착순 쿠폰 발급은 순간적으로 많은 요청이 몰릴 수 있으므로 Redis Lock을 사용해 쿠폰별 발급 로직을 보호합니다.

### 인기검색어

검색어별 점수를 Redis Sorted Set으로 관리할 수 있습니다.

### 채팅 Pub/Sub

다중 서버 환경에서 채팅 메시지를 모든 서버에 전달하기 위해 Redis Pub/Sub을 사용할 수 있습니다.

## API 문서

상세 API 명세는 `docs/API_SPEC.md`를 참고합니다.

## AI / Codex 사용 규칙

AI 에이전트는 `docs/AGENT_RULES.md`를 반드시 참고하여 코드를 생성합니다.

문서별 기준은 다음과 같습니다.

- API 계약: `docs/API_SPEC.md`
- 데이터 구조: `docs/ERD_GUIDE_FOR_CODEX.md`
- 패키지와 책임: `docs/CODE_CONVENTION_USED_MARKET.md`
- 인증·인가: `docs/SECURITY_ARCHITECTURE.md`
- 담당 영역: `docs/DOMAIN_OWNERSHIP.md`

사람과 AI 모두 정책 충돌을 발견하면 코드에서 임의로 결정하지 않고 문서를 먼저 수정합니다.
