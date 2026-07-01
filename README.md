# 5pringUsedMarket
Spring Boot와 JPA를 활용한 중고거래 플랫폼 팀 프로젝트

프로젝트 개요는 이 문서에서 확인하고, AI 작업 시 읽어야 할 공식 문서 순서는 [`docs/AGENT_RULES.md`](docs/AGENT_RULES.md)를 따릅니다.

## 로컬 개발 환경

개발 단계에서는 Spring Boot 애플리케이션은 IntelliJ에서 직접 실행하고, MySQL과 Redis만 Docker Compose로 실행합니다.

```bash
docker compose up -d
```

IntelliJ 실행 환경변수 예시:

```dotenv
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:mysql://localhost:3307/fivespring_used_market
DB_USERNAME=root
DB_PASSWORD=12345678
JWT_SECRET=your-local-jwt-secret-at-least-32-characters
JWT_ACCESS_TOKEN_EXPIRATION=1800000
JWT_REFRESH_TOKEN_EXPIRATION=1209600000
REDIS_HOST=localhost
REDIS_PORT=6379
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=your-s3-bucket-name
AWS_S3_DIRECTORY=products
AWS_S3_MAX_FILE_SIZE=5242880
AWS_ACCESS_KEY_ID=your-aws-access-key-id
AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
```

MySQL 컨테이너 내부 포트는 `3306`이지만 로컬 PC에는 `3307`로 노출합니다. 로컬에 이미 설치된 MySQL이 `3306`을 사용하는 경우가 많기 때문에 포트 충돌을 피하기 위한 구성입니다.

Redis는 로컬 설치와 충돌 가능성이 비교적 낮고 Spring Boot 기본 Redis 포트도 `6379`이므로 `6379:6379`를 사용합니다.

Spring Boot는 `REDIS_HOST`, `REDIS_PORT` 환경변수로 Redis에 연결합니다. `application.yaml`에는 Redis 설정을 두지 않고, `application-local.yml`에서 로컬 Docker Compose 기준 기본값인 `localhost:6379`를 선언합니다. local 프로필은 Parameter Store를 사용하지 않습니다.

`prod` 프로필에서는 AWS Systems Manager Parameter Store의 `/used-market/prod/` 경로를 import합니다. AWS 배포에서는 `REDIS_HOST`에 ElastiCache Redis endpoint를 반드시 등록해야 하며, 값을 누락하면 운영 서버가 실수로 `localhost` Redis에 붙는 문제를 방지하기 위해 기동 설정 오류로 드러나게 합니다. `REDIS_PORT`는 별도 지정이 없으면 `6379`를 사용하고, prod Redis는 TLS를 사용하도록 `spring.data.redis.ssl.enabled=true`로 고정합니다.

## S3 이미지 정책

이 프로젝트의 상품 이미지는 Private S3 Bucket에 저장합니다. 상품 이미지는 사용자 업로드 파일이므로 버킷을 공개하면 URL이 유출되었을 때 장기간 접근을 막기 어렵고, 객체별 접근 제어와 만료 정책을 서버가 통제하기 어렵습니다.

현재 흐름은 다음과 같습니다.

```text
클라이언트
→ POST /api/images/presigned-url 로 업로드용 Presigned PUT URL 발급 요청
→ 응답 data.imageKey, data.uploadUrl 반환
→ 클라이언트가 uploadUrl로 Private S3 Bucket에 직접 PUT 업로드
→ 상품 등록/수정 요청의 imageKeys로 전달
→ DB product_images.image_key에 S3 Object Key만 저장
→ 상품 상세/목록/검색 조회 시 서버가 10분 만료 Presigned URL 생성
→ 응답 imageUrls 또는 thumbnailUrl로 반환
```

DB에는 Public URL을 저장하지 않습니다. Public URL은 버킷 공개 정책에 의존하고, Presigned URL은 만료 시간과 서명이 포함된 임시 조회 URL이라 시간이 지나면 재사용할 수 없습니다. 그래서 영속 데이터에는 안정적인 S3 Object Key만 저장하고, 클라이언트가 이미지를 조회해야 하는 순간에만 서버가 Presigned URL을 생성합니다.

S3 이미지 업로드는 Presigned PUT URL 기반 직접 업로드 방식입니다. 서버는 이미지 파일 바이트를 직접 받지 않고, `S3Presigner`로 10분 동안 유효한 업로드 URL만 발급합니다. 로컬에서는 위 예시처럼 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 환경변수로 둘 수 있고, EC2 운영 환경에서는 `application-prod.yml`에 Access Key를 고정하지 않고 IAM Role 기반 기본 자격 증명 체인을 우선 사용합니다.

현재 S3 업로드 URL 발급 로직은 Spring Cloud AWS가 자동 설정한 AWS SDK v2 `S3Presigner`를 주입받아 사용하는 방식입니다. 별도의 수동 Credentials Bean을 만들지 않으며, Spring Cloud AWS `S3Template`과 혼용하지 않습니다.

Spring Cloud AWS가 사용하는 자격 증명, 리전, 버킷 설정은 `spring.cloud.aws` prefix에 두고, 업로드 디렉터리와 파일 크기 제한 같은 프로젝트 정책값은 `aws.s3` prefix에 둡니다.

S3 관련 환경변수는 다음과 같습니다.

| 환경변수 | 사용 위치 | 설명 |
|---|---|---|
| `AWS_REGION` | `spring.cloud.aws.region.static` | S3 리전, 기본값 `ap-northeast-2` |
| `AWS_S3_BUCKET` | `spring.cloud.aws.s3.bucket` | Private S3 Bucket 이름 |
| `AWS_S3_DIRECTORY` | `aws.s3.directory` | 상품 이미지 Object Key prefix, 기본값 `products` |
| `AWS_S3_MAX_FILE_SIZE` | `aws.s3.max-file-size` | Presigned 업로드 URL 발급 시 허용하는 이미지 최대 크기, 기본값 `5242880` |
| `AWS_ACCESS_KEY_ID` | AWS SDK 기본 자격 증명 체인 | 로컬 개발용 AWS Access Key |
| `AWS_SECRET_ACCESS_KEY` | AWS SDK 기본 자격 증명 체인 | 로컬 개발용 AWS Secret Key |

### Postman S3 직접 업로드 검증 흐름

1. 로그인 후 `accessToken`을 준비한다.
2. `POST {{baseUrl}}/api/images/presigned-url` 요청으로 업로드 URL을 발급받는다.
3. 응답의 `data.uploadUrl`을 `uploadUrl` 변수에, `data.imageKey`를 `imageKey` 변수에 저장한다.
4. `PUT {{uploadUrl}}` 요청을 백엔드가 아니라 S3로 직접 보낸다. 이 요청은 Authorization을 반드시 `No Auth`로 바꾸고, `Content-Type`은 발급 요청의 `contentType`과 동일하게 맞춘다. Body는 form-data나 raw JSON이 아니라 binary/file로 실제 업로드할 이미지 파일을 선택한다.
5. 상품 등록 요청의 `imageKeys`에 `{{imageKey}}`를 전달한다.
6. 상품 상세 조회 응답의 `imageUrls`가 조회용 Presigned GET URL인지 확인한다.

업로드 URL 발급 요청 예시는 다음과 같습니다.

```json
{
  "fileName": "상품 등록 이미지(아이패드).png",
  "contentType": "image/png",
  "fileSize": 1886531
}
```

위 값은 `test-data/images/상품 등록 이미지(아이패드).png` 파일 기준 예시입니다. 다른 파일을 업로드할 때는 `fileName`을 실제 파일명으로, `contentType`을 S3 PUT 요청의 `Content-Type`과 같은 값으로, `fileSize`를 실제 파일 크기(byte)로 바꿔야 합니다. 프론트엔드에서는 브라우저 `File` 객체의 `file.name`, `file.type`, `file.size` 값을 사용합니다.

### Postman S3 직접 업로드 트러블슈팅

- `Only one auth mechanism allowed`: S3 직접 PUT 요청에 Collection Bearer Token이 함께 전송된 경우다. `2-0-1. S3 직접 PUT 업로드` 요청의 Authorization을 `No Auth`로 변경한다.
- `SignatureDoesNotMatch`: Presigned URL 발급 요청의 `fileSize`와 실제 업로드 파일의 `Content-Length`가 다르거나, 발급 요청의 `contentType`과 PUT 요청 Header의 `Content-Type`이 다른 경우다. 실제 파일 byte 크기와 Content-Type을 맞춘 뒤 Presigned URL을 새로 발급받아 재시도한다.
- S3 PUT 요청에서 `form-data` 또는 raw JSON을 사용하면 실패할 수 있다. Body를 binary/file 방식으로 변경한다.

허용 파일 정책은 `jpg`, `jpeg`, `png` 확장자와 `image/jpeg`, `image/png` Content-Type입니다. `webp`는 현재 허용하지 않습니다.

Presigned PUT 방식에서는 서버가 파일 바이트를 직접 보지 않으므로 `ImageIO.read()` 같은 실제 파일 내용 검증을 즉시 수행할 수 없습니다. 현재 구현은 파일명 기반 확장자, 요청 Content-Type, 요청 fileSize, UUID 기반 imageKey 형식을 검증합니다. Content-Type과 확장자는 클라이언트가 위변조할 수 있으므로, 운영 보안을 더 강화하려면 `temp/products/{uuid}.{ext}`에 먼저 업로드하고 S3 Event 또는 Lambda로 파일 시그니처, 실제 MIME Type, 크기, 악성 파일 여부를 검사한 뒤 정상 파일만 `products/{uuid}.{ext}`로 이동하는 후처리 구조가 필요합니다. 이때 `image_uploads` 테이블로 `imageKey`, `memberId`, `status`, `expiresAt`을 관리하는 방식을 검토할 수 있습니다.

브라우저 프론트에서 S3로 직접 PUT 업로드하려면 S3 Bucket CORS 설정이 필요합니다. 운영 체크리스트에는 `AllowedMethods: PUT, GET`, `AllowedHeaders: Content-Type`, 프론트 도메인 `AllowedOrigins`, `ExposeHeaders: ETag`를 포함합니다. Postman 검증에는 S3 CORS 설정이 필요하지 않습니다.

Spring Cloud AWS 의존성은 BOM으로 버전을 통합 관리합니다.

```gradle
implementation platform('io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2')
implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3'
implementation 'io.awspring.cloud:spring-cloud-aws-starter-parameter-store'
```

`prod` 프로필은 Spring Cloud AWS Parameter Store starter로 운영 설정을 읽습니다. starter 버전은 직접 고정하지 않고 Spring Cloud AWS BOM에서 관리합니다.

### 운영 Parameter Store 설정

`application-prod.yml`은 `aws-parameterstore:/used-market/prod/`를 import합니다. EC2 또는 컨테이너 실행 Role에는 최소한 이 경로의 `ssm:GetParameter`, `ssm:GetParameters`, `ssm:GetParametersByPath` 권한이 필요합니다. SecureString을 사용하는 경우 해당 KMS Key의 `kms:Decrypt` 권한도 필요합니다.

| Parameter Store 이름 | Spring 설정 사용처 | 타입 권장 | 필수 |
|---|---|---|---|
| `/used-market/prod/DB_URL` | `spring.datasource.url` | String | Y |
| `/used-market/prod/DB_USERNAME` | `spring.datasource.username` | String | Y |
| `/used-market/prod/DB_PASSWORD` | `spring.datasource.password` | SecureString | Y |
| `/used-market/prod/JWT_SECRET` | `jwt.secret` | SecureString | Y |
| `/used-market/prod/JWT_ACCESS_TOKEN_EXPIRATION` | `jwt.access-token-expiration` | String | N |
| `/used-market/prod/JWT_REFRESH_TOKEN_EXPIRATION` | `jwt.refresh-token-expiration` | String | N |
| `/used-market/prod/REDIS_HOST` | `spring.data.redis.host` | String | Y |
| `/used-market/prod/REDIS_PORT` | `spring.data.redis.port` | String | N |
| `/used-market/prod/AWS_S3_BUCKET` | `spring.cloud.aws.s3.bucket` | String | Y |
| `/used-market/prod/AWS_REGION` | `spring.cloud.aws.region.static` | String | N |
| `/used-market/prod/AWS_S3_DIRECTORY` | `aws.s3.directory` | String | N |
| `/used-market/prod/AWS_S3_MAX_FILE_SIZE` | `aws.s3.max-file-size` | String | N |

현재 단계에서는 빠른 개발과 디버깅을 위해 Spring Boot 컨테이너를 Compose에 포함하지 않습니다. 이후 GitHub Actions나 EC2 배포 단계에서 애플리케이션 컨테이너가 필요해지면 별도 Compose 파일 또는 배포용 프로파일로 확장하는 구조가 적절합니다.

## 배포 브랜치 전략

### 운영 배포 기준

운영 배포 기준 브랜치: `main`

브랜치 흐름은 다음을 기준으로 합니다.

```text
feature/* → develop PR → 통합 테스트 → develop → main PR → main merge 시 운영 배포
```

`develop` 브랜치는 기능 통합 및 테스트 용도입니다. `main` 브랜치는 운영 배포 브랜치입니다. `develop`에 merge해도 운영 EC2 배포는 실행되지 않습니다.

GitHub Actions는 다음 기준으로 동작합니다.

* `develop` 대상 PR: build/test만 수행
* `main` 대상 PR: build/test만 수행
* `main` push/merge: build/test → Docker image build → ECR Push → EC2 Deploy → Health Check

AWS OIDC 인증을 사용하는 경우 IAM Role Trust Policy의 branch 조건도 `refs/heads/main` 기준이어야 합니다. 기존 Trust Policy가 `repo:gpekd5/5pringUsedMarket:ref:refs/heads/develop` 기준이면 `repo:gpekd5/5pringUsedMarket:ref:refs/heads/main` 기준으로 변경해야 합니다. 이 설정이 맞지 않으면 `main` 배포 시 AWS 인증 단계에서 실패할 수 있습니다.

## 배포 헬스체크

배포 후 EC2, Docker, GitHub Actions에서 서버 생존 여부를 확인할 때 아래 Actuator 엔드포인트를 사용합니다.

```http
GET /actuator/health
```

Actuator는 배포 자동화와 인프라 Health Check가 애플리케이션의 기동 상태를 HTTP로 확인할 수 있도록 추가했습니다. 현재는 보안을 위해 `health` 엔드포인트만 노출하고 상세 정보는 숨깁니다.

`/actuator/health`는 비즈니스 API와 달리 인증, 도메인 데이터, 외부 입력 형식에 의존하지 않는 표준 생존 확인 지점입니다. 따라서 EC2 배포 스크립트, Docker 헬스체크, GitHub Actions 배포 검증에서 서버가 정상적으로 떠 있는지 확인하는 용도로 사용합니다.

Redis 연결 여부도 Health Check에서 확인할 수 있도록 컴포넌트 상태는 노출합니다. 정상 연결 시 응답의 `components.redis.status`가 `UP`으로 표시됩니다.

## 로컬 실행 빠른 안내

자세한 로컬 실행 방법은 [`LOCAL_SETUP.md`](LOCAL_SETUP.md)를 참고합니다.

`src/main/resources/application-local.yml`은 개인 로컬 설정 파일이라 Git에 올라가지 않습니다. 처음 실행할 때는 템플릿을 복사해서 생성합니다.

```powershell
Copy-Item .\src\main\resources\application-local.yml.template .\src\main\resources\application-local.yml
```

실행 순서:

```powershell
docker compose up -d mysql redis
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

프론트엔드는 새 터미널에서 실행합니다.

```powershell
cd frontend
npm install
npm run dev
```

접속 주소:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8080
```
