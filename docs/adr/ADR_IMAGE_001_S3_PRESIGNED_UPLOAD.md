# ADR_IMAGE_001: S3 Presigned PUT 이미지 업로드 전환

## 1. 배경

상품 이미지는 Private S3 Bucket에 저장하고, 상품 상세/목록/검색 응답에서는 서버가 `imageKey`를 10분 만료 Presigned GET URL로 변환해 반환한다. DB에는 Public URL이나 Presigned URL을 저장하지 않고 `product_images.image_key`만 저장한다.

## 2. 기존 구조

기존 업로드 API는 `POST /api/images` multipart/form-data 요청으로 `MultipartFile`을 서버에 전달했다. 서버는 파일 크기, Content-Type, 확장자를 검증한 뒤 `S3Client.putObject()`로 Private S3 Bucket에 직접 업로드하고 `imageKey`만 반환했다.

이 방식은 Private Bucket 접근 제어에는 맞지만, 이미지 파일 바이트가 항상 백엔드 서버를 경유하므로 Presigned URL의 서버 트래픽 감소 장점을 충분히 활용하지 못한다.

## 3. 결정

이미지 업로드를 Presigned PUT URL 기반 직접 업로드 방식으로 전환한다.

```text
클라이언트
→ POST /api/images/presigned-url
→ 서버가 imageKey와 10분 만료 uploadUrl 반환
→ 클라이언트가 uploadUrl로 S3에 직접 PUT 업로드
→ 상품 등록/수정 API의 imageKeys에 imageKey 전달
→ DB에는 imageKey만 저장
→ 조회 응답에서는 imageKey를 Presigned GET URL로 변환
```

## 4. 현재 검증 범위

- `fileName` null, blank, 확장자 없음 거부
- `contentType` null, blank 거부
- `fileSize` null 또는 0 이하 거부
- `AWS_S3_MAX_FILE_SIZE` 초과 거부
- `image/jpeg`, `image/png`만 허용
- `jpg`, `jpeg`, `png` 확장자만 허용
- `products/{uuid}.{jpg|jpeg|png}` 형식의 `imageKey`만 상품 등록/수정에서 허용
- URL 문자열과 `products/` 외 prefix 거부

## 5. 보안상 남은 한계

Presigned PUT 방식에서는 서버가 업로드 파일 바이트를 직접 받지 않는다. 따라서 Content-Type은 클라이언트가 전달한 값이라 위변조 가능하고, 확장자도 파일명 변경으로 위변조 가능하다. 현재 구현은 서버 부하와 트래픽을 줄이기 위한 업로드 경로 개선이며, 실제 파일 내용 검증까지 완료하는 구조는 아니다.

## 6. 향후 개선 방향

- `temp/products/{uuid}.{ext}` 임시 prefix에 먼저 업로드
- S3 Event 또는 일정 주기 Lambda로 파일 검증
- 파일 시그니처, 실제 MIME Type, 파일 크기, 악성 파일 여부 검사
- 검증 성공 시 `products/{uuid}.{ext}` 최종 경로로 이동
- 검증 실패 시 삭제
- `image_uploads` 테이블 도입 검토
- `imageKey`, `memberId`, `status(PENDING, APPROVED, REJECTED)`, `expiresAt` 관리
- 상품 등록/수정 API는 APPROVED 상태의 최종 imageKey만 허용

## 7. 이번 범위에서 제외

- Lambda 구현
- S3 Event 설정
- temp/final 이동 로직
- `image_uploads` 테이블 도입
- 브라우저 프론트엔드 구현
- AWS 콘솔 S3 CORS 설정 변경

## 8. Postman 검증 주의사항

Presigned URL 발급 요청은 실제 파일 업로드 요청이 아니라 파일명, Content-Type, 파일 크기만 백엔드에 전달하는 요청이다. Postman 예시는 `test-data/images/상품 등록 이미지(아이패드).png` 기준으로 `fileName=상품 등록 이미지(아이패드).png`, `contentType=image/png`, `fileSize=1886531`을 사용한다.

S3 직접 PUT 요청은 백엔드 API가 아니므로 Collection Bearer Token을 상속하면 안 된다. 해당 요청은 `No Auth`로 설정하고, Body는 form-data나 raw JSON이 아니라 binary/file 방식으로 실제 이미지 파일을 선택한다. `Content-Type`이 발급 요청의 `contentType`과 다르거나 실제 파일 크기가 `fileSize`와 다르면 `SignatureDoesNotMatch`가 발생할 수 있다.
