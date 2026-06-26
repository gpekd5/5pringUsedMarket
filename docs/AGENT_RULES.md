# AGENT_RULES.md

# 1. Source of Truth

항상 아래 순서로 확인한다.

1. docs/
2. README
3. 실제 코드
4. Issue
5. Pull Request

문서와 코드가 다르면 **코드 기준**으로 판단한다.

새로운 정책이나 용어를 발견하면 임의로 추가하지 말고 관련 문서부터 수정 여부를 검토한다.

---

# 2. 구현 원칙

* 기존 구조와 컨벤션을 유지한다.
* 요청 범위를 벗어난 수정은 하지 않는다.
* 불필요한 리팩토링은 하지 않는다.
* Controller는 요청/응답만 담당한다.
* Service는 비즈니스 로직을 담당한다.
* Repository는 데이터 접근만 담당한다.
* Entity를 API 응답으로 직접 반환하지 않는다.
* Request / Response DTO를 분리한다.
* 공통 응답은 `ApiResponse<T>`를 사용한다.
* 예외는 `CustomException + ErrorCode + GlobalExceptionHandler`를 사용한다.

---

# 3. 보안 규칙

* 인증 사용자는 `@AuthenticationPrincipal` 또는 공통 Auth 객체에서 조회한다.
* memberId, senderId 등 인증 정보는 Request Body를 신뢰하지 않는다.
* 권한 검증을 반드시 수행한다.

---

# 4. 테스트

구현 후 반드시 확인한다.

* Build 성공
* Test 성공
* 불필요한 코드 제거
* Warning 확인
* TODO 확인

테스트 코드는 `given / when / then` 구조를 따른다.

---

# 5. Pull Request

* PR 없이 main에 직접 Merge하지 않는다.
* PR 제목은 **한글**로 작성한다.
* PR 본문은 반드시 `.github/PULL_REQUEST_TEMPLATE.md`를 사용한다.

---

# 6. 코드 리뷰

다음 항목을 우선 검토한다.

* API 명세 일치 여부
* 권한 검증
* 예외 처리
* Transaction 범위
* 테스트 코드
* 보안
* 유지보수성

리뷰는 아래 형식을 따른다.

* 문제점
* 위험성
* 수정 방향
