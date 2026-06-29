# API_SPEC.md

## 0. 공통 규칙

### 공통 성공 응답

모든 REST API는 가능하면 아래 응답 구조를 따른다.

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 공통 에러 응답

```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "에러 메시지"
}
```

### 인증 헤더

인증이 필요한 API는 아래 Header를 사용한다.

| Key | Value | 설명 |
|---|---|---|
| Authorization | Bearer {accessToken} | JWT Access Token |

### 주요 Enum

#### ProductStatus

```text
ON_SALE
RESERVED
SOLD
DELETED
```

#### ProductCategory

```text
DIGITAL
FURNITURE
CLOTHING
BOOK
SPORTS
KIDS
BEAUTY
FOOD
PET
ETC
```

#### ChatRoomType

```text
TRADE
CS
```

#### CsStatus

```text
WAITING
IN_PROGRESS
COMPLETED
```

---

# 1. 인증 / 회원 API

## 1-1. 회원가입

- Method: `POST`
- Path: `/api/auth/signup`
- Auth: 불필요

### Request

```json
{
  "email": "test@example.com",
  "password": "Password123!",
  "nickname": "현승"
}
```

### Validation

| 필드 | 규칙 |
|---|---|
| email | 이메일 형식만 허용, 서비스 내 유니크 |
| password | 필수, 저장 시 BCrypt 등 단방향 해시 적용 |
| nickname | 필수, 서비스 내 유니크 |

### Response

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "memberId": 1,
    "email": "test@example.com",
    "nickname": "현승"
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 409 | DUPLICATED_EMAIL | 이미 사용 중인 이메일 |
| 409 | DUPLICATED_NICKNAME | 이미 사용 중인 닉네임 |
| 400 | INVALID_REQUEST | 요청 값 검증 실패 |

---

## 1-2. 로그인

- Method: `POST`
- Path: `/api/auth/login`
- Auth: 불필요

### Request

```json
{
  "email": "test@example.com",
  "password": "Password123!"
}
```

### Response

```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "access-token",
    "refreshToken": "refresh-token",
    "tokenType": "Bearer"
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | INVALID_LOGIN | 이메일 또는 비밀번호 불일치 |
| 400 | INVALID_REQUEST | 요청 값 검증 실패 |

---

## 1-3. 로그아웃

- Method: `POST`
- Path: `/api/auth/logout`
- Auth: 필요

### 처리 정책

- 요청 Header의 Access Token을 추출한다.
- Access Token의 남은 만료 시간을 계산한다.
- 아직 만료되지 않은 Access Token을 Redis Blacklist에 저장한다.
- Redis Blacklist TTL은 Access Token의 남은 만료 시간으로 설정한다.
- Redis Whitelist에 저장된 Refresh Token을 삭제한다.
- Refresh Token 삭제로 이후 토큰 재발급을 차단한다.
- 이후 Blacklist에 존재하는 Access Token으로 요청하면 401을 반환한다.

### Response

```json
{
  "success": true,
  "message": "로그아웃이 완료되었습니다.",
  "data": null
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 401 | EXPIRED_TOKEN | 만료된 Access Token |
| 401 | BLACKLIST_TOKEN | 이미 로그아웃된 토큰 |

---

## 1-4. 토큰 재발급

- Method: `POST`
- Path: `/api/auth/reissue`
- Auth: 불필요

### Request

```json
{
  "refreshToken": "refresh-token"
}
```

### 처리 정책

- Refresh Token 자체의 유효성을 검증한다.
- Redis Whitelist에 저장된 Refresh Token과 요청 Refresh Token을 비교한다.
- 일치하면 새로운 Access Token과 Refresh Token을 발급한다.
- Refresh Token Rotation 전략에 따라 Redis에 저장된 Refresh Token을 새 Refresh Token으로 교체한다.
- 재발급에 사용된 기존 Refresh Token은 더 이상 사용할 수 없다.
- Refresh Token이 만료되었거나 Redis에 없으면 재로그인이 필요하다.

### Response

```json
{
  "success": true,
  "message": "토큰이 재발급되었습니다.",
  "data": {
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token",
    "tokenType": "Bearer"
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | INVALID_REFRESH_TOKEN | 유효하지 않은 Refresh Token |
| 401 | EXPIRED_REFRESH_TOKEN | 만료된 Refresh Token |

---

## 1-5. 내 정보 조회

- Method: `GET`
- Path: `/api/members/me`
- Auth: 필요

### Response

```json
{
  "success": true,
  "message": "내 정보 조회에 성공했습니다.",
  "data": {
    "memberId": 1,
    "email": "test@example.com",
    "nickname": "현승"
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 404 | MEMBER_NOT_FOUND | 회원을 찾을 수 없음 |

---

## 1-6. 내 정보 수정

- Method: `PATCH`
- Path: `/api/members/me`
- Auth: 필요

### Request

```json
{
  "nickname": "새닉네임"
}
```

### Response

```json
{
  "success": true,
  "message": "내 정보가 수정되었습니다.",
  "data": {
    "memberId": 1,
    "nickname": "새닉네임"
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 400 | INVALID_REQUEST | 요청 값 검증 실패 |
| 409 | DUPLICATED_NICKNAME | 이미 사용 중인 닉네임 |

---

## 1-7. 마이페이지 조회

- Method: `GET`
- Path: `/api/mypage`
- Auth: 필요

### 설명

마이페이지 첫 화면에 필요한 요약 정보를 조회한다.  
목록 상세 데이터는 각 도메인 API가 담당한다.

### Response

```json
{
  "success": true,
  "message": "마이페이지 조회에 성공했습니다.",
  "data": {
    "memberId": 1,
    "nickname": "현승",
    "sellingProductCount": 3,
    "wishedProductCount": 5,
    "chatRoomCount": 2,
    "couponCount": 1
  }
}
```

---

# 2-A. 이미지 업로드 API

## 2-A-1. 상품 이미지 업로드

- Method: `POST`
- Path: `/api/images`
- Auth: 필요
- Content-Type: `multipart/form-data`

### Request

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| file | MultipartFile | Y | jpg/jpeg/png/webp 이미지 파일 |

### Validation

| 항목 | 규칙 |
|---|---|
| empty | 빈 파일 거부 |
| MIME type | image/jpeg, image/png, image/webp 허용 |
| extension | jpg, jpeg, png, webp 허용 |
| size | `AWS_S3_MAX_FILE_SIZE` 이하 |

Spring multipart 파서 제한도 `AWS_S3_MAX_FILE_SIZE`와 같은 값으로 맞춘다.

### Response

```json
{
  "success": true,
  "message": "이미지 업로드에 성공했습니다.",
  "data": {
    "imageKey": "products/11111111-1111-1111-1111-111111111111.png"
  }
}
```

### 처리 정책

- S3 Bucket은 Private 정책을 유지한다.
- 업로드 성공 응답은 `imageUrl` 또는 Public S3 URL이 아니라 `imageKey`만 반환한다.
- 현재 서버가 생성하는 `imageKey` 형식은 `products/{uuid}.{jpg|jpeg|png|webp}`이다.
- `products` 디렉터리는 `AWS_S3_DIRECTORY` 기본값 기준이며, 현재 상품 API의 imageKey 검증도 이 형식을 기준으로 한다.
- 상품 조회 응답의 이미지 URL은 서버가 생성한 10분 만료 Presigned URL을 사용한다.

### Error

| Status | Code | 설명 |
|---|---|---|
| 400 | EMPTY_IMAGE_FILE | 빈 이미지 파일 |
| 400 | INVALID_IMAGE_FILE | 지원하지 않는 MIME 타입 또는 확장자 |
| 400 | IMAGE_FILE_SIZE_EXCEEDED | 이미지 파일 크기 제한 초과 |
| 500 | IMAGE_UPLOAD_FAILED | S3 업로드 실패 |

---

# 2. 상품 API

## 2-1. 상품 등록

- Method: `POST`
- Path: `/api/products`
- Auth: 필요

### 처리 정책

- 상품 이미지는 `/api/images`로 먼저 업로드한다.
- 업로드 응답의 `imageKey` 목록을 `imageKeys`로 전달한다.
- 서버는 `products/{uuid}.{jpg|jpeg|png|webp}` 형식의 `imageKey`만 `product_images.image_key`에 저장한다.
- `imageKeys` 항목이 `null`, blank, URL 문자열, `products/` 외 prefix, UUID 파일명 규칙이 아닌 값이면 거부한다.
- 응답의 `imageUrls`는 저장된 `imageKey`를 10분 만료 Presigned URL로 변환한 값이다.

### Request

```json
{
  "title": "MacBook Pro 14인치",
  "price": 2500000,
  "description": "2023년 구매, 상태 매우 좋음",
  "category": "DIGITAL",
  "imageKeys": [
    "products/11111111-1111-1111-1111-111111111111.jpg",
    "products/22222222-2222-2222-2222-222222222222.webp"
  ]
}
```

### Response

```json
{
  "success": true,
  "message": "상품이 등록되었습니다.",
  "data": {
    "productId": 1,
    "sellerId": 42,
    "sellerNickname": "판매자A",
    "title": "MacBook Pro 14인치",
    "price": 2500000,
    "description": "2023년 구매, 상태 매우 좋음",
    "category": "DIGITAL",
    "status": "ON_SALE",
    "imageUrls": [
      "https://bucket-name.s3.ap-northeast-2.amazonaws.com/products/11111111-1111-1111-1111-111111111111.jpg?X-Amz-Signature=...",
      "https://bucket-name.s3.ap-northeast-2.amazonaws.com/products/22222222-2222-2222-2222-222222222222.webp?X-Amz-Signature=..."
    ],
    "wished": false,
    "createdAt": "2026-06-22T10:00:00",
    "updatedAt": "2026-06-22T10:00:00"
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 400 | INVALID_CATEGORY | 유효하지 않은 카테고리 |
| 400 | INVALID_PRICE | 가격은 0 이상이어야 함 |
| 400 | INVALID_IMAGE_KEY | 허용되지 않는 imageKey |
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |

---

## 2-2. 상품 목록 조회

- Method: `GET`
- Path: `/api/products`
- Auth: 불필요

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| category | String | N | 카테고리 필터 |
| keyword | String | N | 상품 제목 검색 키워드 |
| sellerId | Long | N | 특정 판매자의 상품 필터 |
| status | String | N | 판매 상태 필터, 기본값 ON_SALE |
| page | Integer | N | 기본값 0 |
| size | Integer | N | 기본값 20 |
| sort | String | N | 예: createdAt,desc |

### Response

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "productId": 1,
        "sellerId": 42,
        "title": "MacBook Pro 14인치",
        "price": 2500000,
        "category": "DIGITAL",
        "status": "ON_SALE",
        "thumbnailUrl": "https://bucket-name.s3.ap-northeast-2.amazonaws.com/products/11111111-1111-1111-1111-111111111111.jpg?X-Amz-Signature=...",
        "createdAt": "2026-06-22T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 처리 정책

- `thumbnailUrl`은 DB에 저장된 `imageKey`를 10분 만료 Presigned URL로 변환한 값이다.
- 대표 이미지가 없으면 `thumbnailUrl`은 `null`이다.
- `status`가 생략되면 `ON_SALE` 상품을 조회한다.
- `DELETED` 상태는 공개 목록에서 조회할 수 없다.

---

## 2-3. 상품 상세 조회

- Method: `GET`
- Path: `/api/products/{productId}`
- Auth: 선택

### 설명

현재 구현은 인증 여부와 관계없이 `wished = false`로 반환한다.

### Response

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {
    "productId": 1,
    "sellerId": 3,
    "sellerNickname": "판매자A",
    "title": "아이폰 15 팝니다",
    "price": 800000,
    "description": "상태 좋습니다.",
    "category": "DIGITAL",
    "status": "ON_SALE",
    "imageUrls": [
      "https://bucket-name.s3.ap-northeast-2.amazonaws.com/products/11111111-1111-1111-1111-111111111111.png?X-Amz-Signature=...",
      "https://bucket-name.s3.ap-northeast-2.amazonaws.com/products/22222222-2222-2222-2222-222222222222.png?X-Amz-Signature=..."
    ],
    "wished": false,
    "createdAt": "2026-06-23T10:00:00",
    "updatedAt": "2026-06-23T10:00:00"
  }
}
```

### 처리 정책

- `imageUrls`는 DB에 저장된 `imageKey` 목록을 10분 만료 Presigned URL로 변환한 값이다.
- DB에 저장된 `imageKey` 원문은 상세 조회 응답에 노출하지 않는다.
- 현재 구현의 `wished` 값은 `false`로 반환된다.

### Error

| Status | Code | 설명 |
|---|---|---|
| 404 | PRODUCT_NOT_FOUND | 상품을 찾을 수 없음 |

---

## 2-4. 상품 정보 수정

- Method: `PATCH`
- Path: `/api/products/{productId}`
- Auth: 필요

### Request

```json
{
  "title": "MacBook Pro 14인치 (수정)",
  "price": 2300000,
  "description": "가격 인하합니다",
  "category": "DIGITAL",
  "imageKeys": [
    "products/33333333-3333-3333-3333-333333333333.jpg"
  ]
}
```

### 처리 정책

- 전달된 필드만 수정하며 `null` 필드는 기존 값을 유지한다.
- `imageKeys`가 `null`이거나 필드가 없으면 기존 이미지 목록을 유지한다.
- `imageKeys`가 빈 배열이면 기존 이미지를 모두 삭제한다.
- `imageKeys`에 값이 있으면 기존 이미지 목록을 삭제하고 요청 순서대로 새 `imageKey` 목록을 저장한다.
- 수정 응답의 `imageUrls`는 저장된 `imageKey`를 10분 만료 Presigned URL로 변환한 값이다.

### Response

```json
{
  "success": true,
  "message": "상품이 수정되었습니다.",
  "data": {
    "productId": 1,
    "sellerId": 42,
    "sellerNickname": "판매자A",
    "title": "MacBook Pro 14인치 (수정)",
    "price": 2300000,
    "description": "가격 인하합니다",
    "category": "DIGITAL",
    "status": "ON_SALE",
    "imageUrls": [
      "https://bucket-name.s3.ap-northeast-2.amazonaws.com/products/33333333-3333-3333-3333-333333333333.jpg?X-Amz-Signature=..."
    ],
    "wished": false,
    "createdAt": "2026-06-22T10:00:00",
    "updatedAt": "2026-06-23T11:30:00"
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 400 | INVALID_CATEGORY | 유효하지 않은 카테고리 |
| 400 | INVALID_IMAGE_KEY | 허용되지 않는 imageKey |
| 400 | CANNOT_MODIFY_SOLD_PRODUCT | SOLD 상태 상품 수정 불가 |
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 403 | FORBIDDEN | 본인 상품이 아님 |
| 404 | PRODUCT_NOT_FOUND | 상품을 찾을 수 없음 |

---

## 2-5. 상품 삭제

- Method: `DELETE`
- Path: `/api/products/{productId}`
- Auth: 필요

### 처리 정책

- 물리 삭제가 아니라 상태를 `DELETED`로 변경하는 소프트 삭제를 우선한다.

### Response

```json
{
  "success": true,
  "message": "상품이 삭제되었습니다.",
  "data": null
}
```

---

## 2-6. 내 판매 상품 목록 조회

- Method: `GET`
- Path: `/api/products/me`
- Auth: 필요

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | String | N | ON_SALE / RESERVED / SOLD |
| page | Integer | N | 기본값 0 |
| size | Integer | N | 기본값 20 |

---

## 2-7. 판매자 프로필 조회

- Method: `GET`
- Path: `/api/members/{memberId}/profile`
- Auth: 불필요

### Response

```json
{
  "success": true,
  "message": "판매자 프로필 조회에 성공했습니다.",
  "data": {
    "memberId": 42,
    "nickname": "판매왕",
    "productCount": 5
  }
}
```

판매자의 상품 목록은 `GET /api/products?sellerId={memberId}`로 페이징하여 조회한다.

---

## 2-8. 상품 상태 변경

- Method: `PATCH`
- Path: `/api/products/{productId}/status`
- Auth: 필요

### Request

```json
{
  "status": "RESERVED"
}
```

### 상태 전이 정책

- `ON_SALE -> RESERVED`
- `ON_SALE -> SOLD`
- `RESERVED -> SOLD`
- `RESERVED -> ON_SALE`은 예약 취소 API 사용
- `SOLD -> ON_SALE`, `SOLD -> RESERVED` 불가

### Error

| Status | Code | 설명 |
|---|---|---|
| 400 | INVALID_STATUS | 유효하지 않은 상태 |
| 400 | INVALID_STATUS_TRANSITION | 허용되지 않는 상태 전이 |
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 403 | FORBIDDEN | 본인 상품이 아님 |
| 404 | PRODUCT_NOT_FOUND | 상품을 찾을 수 없음 |

---

## 2-9. 예약 취소

- Method: `PATCH`
- Path: `/api/products/{productId}/status/cancel-reservation`
- Auth: 필요

### 처리 정책

- `RESERVED` 상태인 상품만 `ON_SALE`로 변경할 수 있다.

---

# 3. 쿠폰 API

## 3-1. 이벤트 쿠폰 목록 조회

- Method: `GET`
- Path: `/api/coupons`
- Auth: 불필요

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| page | Integer | N | 기본값 0 |
| size | Integer | N | 기본값 20 |

---

## 3-2. 선착순 쿠폰 발급

- Method: `POST`
- Path: `/api/coupons/{couponId}/issue`
- Auth: 필요

### 처리 정책

- Redis Lock을 이용해 쿠폰별 발급 경쟁을 제어한다.
- Lock Key 예시: `lock:coupon:{couponId}`
- 발급 조건: 이벤트 기간, 잔여 수량, 중복 발급 여부
- 성공 시 `user_coupons` 저장 및 쿠폰 잔여 수량 차감
- 실패 또는 성공 후 Lock을 해제한다.

### Response

```json
{
  "success": true,
  "message": "쿠폰 발급에 성공했습니다.",
  "data": {
    "userCouponId": 55,
    "couponId": 1,
    "couponName": "신규가입 환영 쿠폰",
    "code": "WELCOME-A3F9K2",
    "issuedAt": "2026-06-22T14:00:00",
    "expireAt": "2026-07-31T23:59:59",
    "usedAt": null
  }
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 404 | COUPON_NOT_FOUND | 쿠폰을 찾을 수 없음 |
| 409 | COUPON_ALREADY_ISSUED | 이미 발급받은 쿠폰 |
| 410 | COUPON_OUT_OF_STOCK | 쿠폰 수량 소진 |
| 422 | COUPON_EVENT_NOT_STARTED | 이벤트 시작 전 |
| 422 | COUPON_EVENT_ENDED | 이벤트 종료 |

---

## 3-3. 내 쿠폰 목록 조회

- Method: `GET`
- Path: `/api/members/me/coupons`
- Auth: 필요

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| used | Boolean | N | true: 사용완료, false: 미사용, 생략: 전체 |
| page | Integer | N | 기본값 0 |
| size | Integer | N | 기본값 20 |

---

## 3-4. 쿠폰 사용 처리

- Method: `PATCH`
- Path: `/api/user-coupons/{userCouponId}/use`
- Auth: 필요

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 403 | FORBIDDEN | 본인 쿠폰이 아님 |
| 404 | USER_COUPON_NOT_FOUND | 유저 쿠폰을 찾을 수 없음 |
| 409 | COUPON_ALREADY_USED | 이미 사용된 쿠폰 |
| 422 | COUPON_EXPIRED | 만료된 쿠폰 |

---

# 4. 검색 / 인기검색어 API

## 검색 정책 요약
* 상품 검색 API는 로그인/비로그인 사용자 모두 사용할 수 있다.
* 비로그인 사용자는 상품 검색 결과만 조회할 수 있다.
* 로그인 사용자가 `keyword`로 검색한 경우에만 검색어를 기록한다.
* 검색어 기록은 두 가지 목적으로 나누어 관리한다.

```text
SearchLog DB
→ 로그인 사용자의 최근 검색어 조회/삭제용

Redis ZSet
→ 로그인 사용자 검색어 기반 인기검색어 Top 10 조회용
```

* `keyword`가 없거나 공백 문자열인 경우 검색어를 저장하지 않는다.
* 검색어 저장 시 앞뒤 공백은 `trim` 처리한다.
* 동일 사용자가 같은 검색어를 여러 번 검색해도 검색 기록은 모두 저장한다.
* 최근 검색어 조회 시에는 같은 keyword가 여러 개 있더라도 최신 기준으로 하나만 노출한다.
* 인기검색어는 Redis ZSet의 score 기준 상위 10개만 노출한다.

## 4-1. 상품 검색 v1

- Method: `GET`
- Path: `/api/v1/products/search`
- Auth: 선택
- Cache: 없음

### Query Parameters

| 필드 | 타입 | 필수 | 설명                                       |
|---|---|---|------------------------------------------|
| keyword | String | N | 검색 키워드                                   |
| category | String | N | 상품 카테고리                                  |
| status | String | N | 판매 상태 필터, 기본값 ON_SALE |
| sort | String | N | latest / oldest / price_asc / price_desc |
| page | Integer | N | 기본값 0                                    |
| size | Integer | N | 기본값 10                                   |

### 처리 정책

* 상품명 또는 설명 컬럼에 `LIKE` 검색을 수행한다.
* 기본적으로 `DELETED` 상태의 상품은 검색 결과에 노출하지 않는다.
* `status`가 생략되면 기본적으로 `ON_SALE` 상품을 조회한다.
* 로그인/비로그인 사용자 모두 상품 검색이 가능하다.
* 로그인 사용자가 `keyword`로 검색한 경우에만 검색어를 기록한다.
* 비로그인 사용자가 검색한 경우에는 상품 검색만 수행하고 검색어 기록은 저장하지 않는다.
* `keyword`가 없거나 공백 문자열이면 검색어 기록을 저장하지 않는다.
* 검색어 기록 저장 시 앞뒤 공백은 제거한다.
* 로그인 사용자의 검색어는 `search_logs`에 저장한다.
* 로그인 사용자의 검색어는 Redis ZSet에도 반영하여 인기검색어 집계에 사용한다.
* 검색 Repository는 대표 이미지의 `imageKey`를 조회하고, Service에서 `thumbnailUrl`을 10분 만료 Presigned URL로 변환해 반환한다.
* 대표 이미지가 없으면 `thumbnailUrl`은 `null`이다.

### 검색어 기록 처리

```text
비로그인 사용자 검색
→ 상품 검색 가능
→ search_logs 저장 안 함
→ Redis 인기검색어 집계 안 함

로그인 사용자 검색 + keyword 있음
→ 상품 검색 가능
→ search_logs 저장
→ Redis ZSet score 증가

로그인 사용자 검색 + keyword 없음
→ 상품 검색 가능
→ search_logs 저장 안 함
→ Redis 인기검색어 집계 안 함
```

### Response

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "productId": 1,
        "sellerId": 42,
        "title": "MacBook Pro 14인치",
        "price": 2500000,
        "category": "DIGITAL",
        "status": "ON_SALE",
        "thumbnailUrl": "https://bucket-name.s3.ap-northeast-2.amazonaws.com/products/11111111-1111-1111-1111-111111111111.jpg?X-Amz-Signature=...",
        "createdAt": "2026-06-22T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 4-2. 상품 검색 v2

- Method: `GET`
- Path: `/api/v2/products/search`
- Auth: 선택
- Cache: Caffeine Local Memory Cache

### 처리 정책

* v1과 동일한 검색 결과를 반환한다.
* 동일한 검색 조건의 반복 요청은 Cache에서 응답할 수 있다.
* 캐시 Key는 keyword, category, status, sort, page, size를 포함해야 한다.
* 상품 수정/삭제/상태 변경 시 캐시 무효화 전략을 고려한다.
* 검색어 기록 저장 정책은 v1과 동일하다.
* 로그인 사용자가 `keyword`로 검색한 경우에만 `search_logs` 저장 및 Redis 인기검색어 집계를 수행한다.
* 비로그인 검색은 검색어 기록과 인기검색어 집계에 반영하지 않는다.
* 응답 구조는 v1과 동일하며, `thumbnailUrl`은 10분 만료 Presigned URL이다.

---
## 4-3. 상품 검색 v3

* Method: `GET`
* Path: `/api/v3/products/search`
* Auth: 선택
* Cache: Redis Remote Cache

### Query Parameters

| 필드       | 타입      | 필수 | 설명                                       |
| -------- | ------- | -- | ---------------------------------------- |
| keyword  | String  | N  | 검색 키워드                                   |
| category | String  | N  | 상품 카테고리                                  |
| status   | String  | N  | 판매 상태 필터, 기본값 ON_SALE                    |
| sort     | String  | N  | latest / oldest / price_asc / price_desc |
| page     | Integer | N  | 기본값 0                                    |
| size     | Integer | N  | 기본값 10                                   |

### 처리 정책

* v1, v2와 동일한 검색 결과를 반환한다.
* 동일한 검색 조건의 반복 요청은 Redis Cache에서 응답할 수 있다.
* Redis Cache는 애플리케이션 외부의 원격 캐시 저장소로 사용한다.
* Scale-out 환경에서 여러 애플리케이션 서버가 동일한 검색 결과 캐시를 공유할 수 있다.
* 캐시 전략은 Cache-aside 방식을 사용한다.
* Cache Hit 시 Redis에 저장된 검색 결과를 반환한다.
* Cache Miss 시 DB를 조회하고, 조회 결과를 Redis에 저장한 뒤 응답한다.
* 검색 결과 캐시는 Redis String 자료구조를 사용한다.
* 캐시 Key는 keyword, category, status, sort, page, size를 포함해야 한다.
* 캐시 TTL은 5분을 기준으로 한다.
* 상품 수정/삭제/상태 변경 시 Redis 검색 결과 캐시 무효화 전략을 고려한다.
* 검색어 기록 저장 정책은 v1, v2와 동일하다.
* 로그인 사용자가 `keyword`로 검색한 경우에만 `search_logs` 저장 및 Redis 인기검색어 집계를 수행한다.
* 비로그인 검색은 검색어 기록과 인기검색어 집계에 반영하지 않는다.
* 검색어 기록 저장과 인기검색어 집계는 캐시 Hit 여부와 관계없이 수행되어야 한다.
* 따라서 검색어 기록 저장 로직은 `@Cacheable`이 적용된 검색 결과 조회 메서드 내부에 두지 않는다.

### Redis Cache 처리 흐름

```text
상품 검색 v3 요청
→ 검색 조건 파싱
→ 로그인 사용자 + keyword 존재 시 search_logs 저장
→ 로그인 사용자 + keyword 존재 시 Redis ZSet 인기검색어 score 증가
→ Redis Cache 조회
→ Cache Hit: Redis 검색 결과 반환
→ Cache Miss: DB 조회
→ DB 조회 결과를 Redis String value로 저장
→ 검색 결과 응답
```

### Redis Cache Key 예시

```text
productSearch::keyword=맥북:category=DIGITAL:status=ON_SALE:sort=latest:page=0:size=10
```

### Response

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "productId": 1,
        "sellerId": 42,
        "title": "MacBook Pro 14인치",
        "price": 2500000,
        "category": "DIGITAL",
        "status": "ON_SALE",
        "thumbnailUrl": "https://cdn.example.com/images/product1.jpg",
        "createdAt": "2026-06-22T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 4-4. 인기검색어 Top N 조회

- Method: `GET`
- Path: `/api/search/popular`
- Auth: 불필요

### Query Parameters

| 필드 | 타입 | 필수 | 설명    |
|---|---|---|-------|

### 처리 정책

* 로그인 사용자가 검색한 keyword를 기준으로 인기검색어를 집계한다.
* 비로그인 사용자의 검색어는 인기검색어 집계에 반영하지 않는다.
* Redis Sorted Set, ZSet을 사용한다.
* Redis ZSet의 key는 `popular:keywords`를 사용한다.
* 검색어는 ZSet의 member로 저장한다.
* 검색 횟수는 ZSet의 score로 저장한다.
* 검색어가 기록될 때마다 `ZINCRBY`를 사용해 score를 1 증가시킨다.
* 인기검색어 조회 시 `ZREVRANGE` 또는 `reverseRangeWithScores`를 사용해 score가 높은 순서로 조회한다.
* 인기검색어는 상위 10개만 반환한다.
* 인기검색어가 없으면 빈 배열을 반환한다.

---

## 4-5. 최근 검색어 조회

- Method: `GET`
- Path: `/api/search/recent`
- Auth: 필요

### Query Parameters

없음

### 처리 정책

* 최근 검색어는 로그인 사용자 본인의 검색어만 조회한다.
* 인증 Principal의 memberId를 기준으로 `search_logs`를 조회한다.
* Request Body나 Query Parameter로 memberId를 받지 않는다.
* 검색 기록은 최신순으로 조회한다.
* 같은 keyword가 여러 번 존재하더라도 응답에는 하나만 노출한다.
* 중복 keyword는 가장 최근 검색 기록을 기준으로 남긴다.
* 최근 검색어는 최대 10개만 반환한다.
* 검색 기록이 없으면 빈 배열을 반환한다.

### Response

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "searchLogId": 15,
      "keyword": "맥북",
      "createdAt": "2026-06-25T13:00:00"
    },
    {
      "searchLogId": 12,
      "keyword": "아이폰",
      "createdAt": "2026-06-25T12:50:00"
    }
  ]
}
```

---

## 4-6. 최근 검색어 삭제

* Method: `DELETE`
* Path: `/api/search/recent/{searchLogId}`
* Auth: 필요

### 처리 정책

* 최근 검색어는 본인의 검색 기록만 삭제할 수 있다.
* 인증 Principal의 memberId와 `search_logs.member_id`가 일치해야 한다.
* 다른 사용자의 검색 기록 삭제 요청은 403을 반환한다.
* 존재하지 않는 검색 기록이면 404를 반환한다.
* 삭제 방식은 물리 삭제 또는 소프트 삭제 중 하나로 통일한다.

### Response

```json
{
  "success": true,
  "message": "최근 검색어가 삭제되었습니다.",
  "data": null
}
```

### Error

| Status | Code                 | 설명             |
| ------ | -------------------- | -------------- |
| 401    | UNAUTHORIZED         | 인증되지 않은 사용자    |
| 403    | FORBIDDEN            | 본인의 검색 기록이 아님  |
| 404    | SEARCH_LOG_NOT_FOUND | 검색 기록을 찾을 수 없음 |

---

# 5. 관심상품 API

## 관심상품 정책

### 공통 정책

- 관심상품 기능은 로그인 사용자만 사용할 수 있다.
- 비로그인 사용자는 관심상품 등록, 취소, 목록 조회를 할 수 없다.
- 관심상품은 `member_id + product_id` 조합으로 관리한다.
- 같은 회원은 같은 상품을 중복으로 관심 등록할 수 없다.
- DB 레벨에서도 `member_id + product_id` Unique 제약을 둔다.

## 5-1. 관심상품 등록

- Method: `POST`
- Path: `/api/products/{productId}/wishes`
- Auth: 필요

### 처리 정책

- 존재하지 않는 상품은 관심 등록할 수 없다.
- `DELETED` 상태의 상품은 관심 등록할 수 없다.
- 본인이 등록한 상품은 관심 등록할 수 없다.
- `ON_SALE`, `RESERVED`, `SOLD` 상태의 상품은 관심 등록할 수 있다.
- 이미 관심 등록한 상품을 다시 등록하려고 하면 예외를 반환한다.

### Response

```json
{
  "success": true,
  "message": "관심상품으로 등록되었습니다.",
  "data": {
    "productId": 1,
    "wished": true
  }
}
```

---

## 5-2. 관심상품 취소

- Method: `DELETE`
- Path: `/api/products/{productId}/wishes`
- Auth: 필요

### 처리 정책
- 본인이 관심 등록한 상품만 취소할 수 있다.
- 관심 등록하지 않은 상품을 취소하려고 하면 예외를 반환한다.
- 존재하지 않는 상품에 대한 취소 요청은 예외를 반환한다.

### Response

```json
{
  "success": true,
  "message": "관심상품이 취소되었습니다.",
  "data": {
    "productId": 1,
    "wished": false
  }
}
```

---

## 5-3. 관심상품 목록 조회

- Method: `GET`
- Path: `/api/members/me/wishes`
- Auth: 필요

### Query Parameters

없음

### 처리 정책

- 로그인 사용자의 관심상품 목록만 조회한다.
- 인증 Principal의 `memberId`를 기준으로 조회한다.
- Request Body나 Query Parameter로 `memberId`를 받지 않는다.
- 다른 사용자의 관심상품 목록은 조회할 수 없다.
- 최신 관심 등록순으로 조회한다.
- `DELETED` 상태의 상품은 목록에서 제외한다.
- `SOLD` 상태의 상품은 목록에 포함한다.
- 관심상품이 없으면 빈 배열을 반환한다.
- 응답에는 관심 등록 시각인 `wishedAt`을 포함한다.
- 목록 조회 결과는 전부 관심 등록된 상품이므로 별도의 `wished` 값은 반환하지 않는다.

### Response

```json
{
  "success": true,
  "message": "관심상품 목록 조회에 성공했습니다.",
  "data": [
    {
      "productId": 1,
      "title": "아이폰 15 팝니다",
      "price": 800000,
      "category": "DIGITAL",
      "status": "ON_SALE",
      "thumbnailUrl": "https://image-url.com/1.png",
      "wishedAt": "2026-06-26T15:30:00"
    },
    {
      "productId": 2,
      "title": "맥북 팝니다",
      "price": 1200000,
      "category": "DIGITAL",
      "status": "SOLD",
      "thumbnailUrl": "https://image-url.com/2.png",
      "wishedAt": "2026-06-25T10:12:00"
    }
  ]
}
```

### Empty Response

```json
{
  "success": true,
  "message": "관심상품 목록 조회에 성공했습니다.",
  "data": []
}
```

### Error

| Status | Code | 설명 |
|---|---|---|
| 401 | UNAUTHORIZED | 인증되지 않은 사용자 |
| 404 | MEMBER_NOT_FOUND | 회원을 찾을 수 없음 |
```

---

# 6. 채팅 API

## 6-1. 거래 채팅방 생성 또는 조회

- Method: `POST`
- Path: `/api/chat/rooms/trade`
- Auth: 필요

### Request

```json
{
  "productId": 1
}
```

### 처리 정책

- 기존 채팅방이 있으면 기존 roomId를 반환한다.
- 기존 채팅방이 없으면 새 채팅방을 생성한다.
- 본인 상품에는 거래 채팅을 시작할 수 없다.
- 판매완료 상품에는 거래 채팅을 시작할 수 없다.

### Response

```json
{
  "success": true,
  "message": "채팅방 생성 성공",
  "data": {
    "roomId": 42,
    "type": "TRADE",
    "isNew": true,
    "product": {
      "id": 1,
      "title": "자전거 팝니다",
      "price": 120000,
      "thumbnailUrl": "https://..."
    },
    "counterpart": {
      "memberId": 2,
      "nickname": "판매자Kim"
    },
    "createdAt": "2026-06-22T10:00:00"
  }
}
```

---

## 6-2. CS 채팅방 생성

- Method: `POST`
- Path: `/api/chat/rooms/cs`
- Auth: 필요

### Request

```json
{
  "title": "결제 오류 문의"
}
```

### 처리 정책

- 항상 새 채팅방을 생성한다.
- 생성 시 `csStatus = WAITING`으로 설정한다.

---

## 6-3. 채팅방 목록 조회

- Method: `GET`
- Path: `/api/chat/rooms`
- Auth: 필요

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| type | String | N | TRADE / CS |
| page | Integer | N | 기본값 0 |
| size | Integer | N | 기본값 10 |

---

## 6-4. 채팅방 상세 조회

- Method: `GET`
- Path: `/api/chat/rooms/{roomId}`
- Auth: 필요

### 처리 정책

- 채팅방 참여자만 조회 가능하다.
- `type = TRADE`이면 상품 정보와 상대방 정보를 반환한다.
- `type = CS`이면 문의 제목, 문의 상태, 상대방 정보를 반환한다.

---

## 6-5. 메시지 목록 조회

- Method: `GET`
- Path: `/api/chat/rooms/{roomId}/messages`
- Auth: 필요

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| lastMessageId | Long | N | 커서 기준 메시지 ID. 없으면 최신 메시지 조회 |
| size | Integer | N | 기본값 20 |

### 처리 정책

- 커서 기반 페이징을 사용한다.
- 처음 입장 시 최신 N개를 반환한다.
- 스크롤 위로 올릴 때 `lastMessageId` 기준으로 이전 메시지를 조회한다.
- 응답은 클라이언트 렌더링 편의를 위해 시간 오름차순으로 정렬할 수 있다.

---

## 6-6. 메시지 읽음 처리

- Method: `PATCH`
- Path: `/api/chat/rooms/{roomId}/read`
- Auth: 필요

### Request

```json
{
  "lastReadMessageId": 990
}
```

### 처리 정책

- 채팅방 참여자만 처리할 수 있다.
- 요청 사용자의 `chat_members.last_read_message_id`를 갱신한다.
- memberId는 Request Body에서 받지 않고 인증 Principal에서 가져온다.

---

## 6-7. CS 채팅 상태 변경

- Method: `PATCH`
- Path: `/api/admin/chat/rooms/{roomId}/cs-status`
- Auth: 필요

### Request

```json
{
  "status": "IN_PROGRESS"
}
```

### 상태 전이 정책

- `WAITING -> IN_PROGRESS`
- `IN_PROGRESS -> COMPLETED`
- `COMPLETED -> WAITING` 불가

---

## 6-8. STOMP 메시지 송신

- Protocol: `WebSocket + STOMP`
- Send Destination: `/pub/chat/rooms/{roomId}/messages`
- Subscribe Destination: `/sub/chat/rooms/{roomId}`

### CONNECT 인증

- STOMP CONNECT 시점에 `Authorization: Bearer {accessToken}`을 전달한다.
- `ChannelInterceptor`에서 JWT를 검증한다.
- 인증된 사용자를 `Principal`로 설정한다.
- 메시지 송신 시 클라이언트가 senderId를 보내지 않는다.
- 서버가 Principal에서 senderId를 식별한다.

### Message Request

```json
{
  "content": "직거래 가능한가요?"
}
```

### Message Response

```json
{
  "messageId": 990,
  "roomId": 42,
  "senderId": 1,
  "senderNickname": "박동네",
  "content": "직거래 가능한가요?",
  "createdAt": "2026-06-22T10:42:00"
}
```

### 처리 정책

- 메시지는 DB에 저장한다.
- 단일 서버는 STOMP broker로 전달한다.
- 다중 서버 확장 시 Redis Pub/Sub을 사용한다.
