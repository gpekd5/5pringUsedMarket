# AGENT_RULES.md

AI 에이전트가 이 프로젝트에서 작업할 때 따라야 할 문서 확인 규칙이다.
프로젝트 정책의 Source of Truth는 각 도메인 문서에 두고, 이 문서에는 정책 본문을 중복해서 적지 않는다.

---

# 1. 기본 확인 순서

작업을 시작하기 전에 아래 순서로 확인한다.

1. `README.md`
2. `docs/PROJECT_CONTEXT.md`
3. 작업 도메인에 해당하는 공식 문서
4. 실제 코드
5. 관련 Issue / Pull Request

문서와 코드가 다르면 현재 동작은 코드 기준으로 판단하되, 최종 수정에서는 코드와 문서가 다시 일치하도록 함께 정리한다.

---

# 2. 작업별 필수 문서

| 작업 유형 | 먼저 읽을 문서 |
|---|---|
| API 요청/응답, 에러 코드, 인증 필요 여부 | `docs/API_SPEC.md` |
| Entity, 테이블, 컬럼, 연관관계, DB 전환 정책 | `docs/ERD_GUIDE_FOR_CODEX.md` |
| 인증, 인가, JWT, Redis Token, 리소스 접근 제어 | `docs/SECURITY_ARCHITECTURE.md` |
| 패키지 위치, 계층 책임, 도메인 경계 | `docs/PACKAGE_STRUCTURE.md`, `docs/DOMAIN_OWNERSHIP.md` |
| 코드 스타일, DTO/응답/예외/테스트 컨벤션 | `docs/CODE_CONVENTION_USED_MARKET.md` |
| 검색 v1/v2, Querydsl, Caffeine Cache | `docs/adr/ADR_SEARCH_001_QUERYDSL_SEARCH_V1.md`, `docs/adr/ADR_SEARCH_002_CAFFEINE_CACHE_V2.md` |
| 검색 장애 대응 또는 캐시 이슈 | `docs/troubleshooting/` 하위 문서 |
| S3 이미지 업로드, imageKey, Presigned URL | `docs/API_SPEC.md`, `docs/ERD_GUIDE_FOR_CODEX.md`, `docs/SECURITY_ARCHITECTURE.md` |

---

# 3. 충돌 처리

문서끼리 내용이 다르면 다음 순서로 우선한다.

1. 실제 코드와 테스트
2. 더 구체적인 도메인 문서
3. `README.md`와 `PROJECT_CONTEXT.md`
4. Issue / Pull Request 설명

충돌을 발견하면 한쪽만 수정하지 않는다.
코드 변경이 있으면 관련 공식 문서도 함께 갱신하고, 문서만 틀린 경우에는 코드 기준으로 문서를 수정한다.

---

# 4. 작업 원칙

* 요청 범위를 먼저 확인하고, 범위를 벗어난 변경은 별도 PR 후보로 기록한다.
* 기존 구조와 컨벤션을 우선한다.
* 보안, 권한 검증, 데이터 정합성, 운영 장애 가능성을 단순 스타일보다 우선한다.
* 인증 사용자 식별자는 Request Body를 신뢰하지 않고 서버 인증 컨텍스트에서 가져온다.
* Entity를 API 응답으로 직접 반환하지 않는다.
* 예외와 응답 형식은 프로젝트 공통 문서와 기존 구현을 따른다.

---

# 5. 완료 전 점검

* 관련 테스트를 실행한다.
* API 명세, ERD, 보안 문서가 실제 코드와 일치하는지 확인한다.
* 불필요한 변경이 같은 PR에 섞였는지 확인한다.
* 같은 PR에 남겨야 하는 부수 변경은 PR 본문에 이유를 적는다.
