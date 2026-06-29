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
→ POST /api/images multipart 업로드
→ 서버가 Private S3 Bucket에 저장
→ 응답 data.imageKey 반환
→ 상품 등록/수정 요청의 imageKeys로 전달
→ DB product_images.image_key에 S3 Object Key만 저장
→ 상품 상세/목록/검색 조회 시 서버가 10분 만료 Presigned URL 생성
→ 응답 imageUrls 또는 thumbnailUrl로 반환
```

DB에는 Public URL을 저장하지 않습니다. Public URL은 버킷 공개 정책에 의존하고, Presigned URL은 만료 시간과 서명이 포함된 임시 조회 URL이라 시간이 지나면 재사용할 수 없습니다. 그래서 영속 데이터에는 안정적인 S3 Object Key만 저장하고, 클라이언트가 이미지를 조회해야 하는 순간에만 서버가 Presigned URL을 생성합니다.

S3 이미지 업로드는 Spring Cloud AWS S3 starter의 자동 설정을 사용합니다. 로컬에서는 위 예시처럼 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 환경변수로 둘 수 있고, EC2 운영 환경에서는 `application-prod.yml`에 Access Key를 고정하지 않고 IAM Role 기반 기본 자격 증명 체인을 우선 사용합니다.

현재 S3 업로드 로직은 Spring Cloud AWS가 자동 설정한 AWS SDK v2 `S3Client`를 주입받아 사용하는 방식입니다. 별도의 수동 Credentials Bean을 만들지 않으며, Spring Cloud AWS `S3Template`과 혼용하지 않습니다.

Spring Cloud AWS가 사용하는 자격 증명, 리전, 버킷 설정은 `spring.cloud.aws` prefix에 두고, 업로드 디렉터리와 파일 크기 제한 같은 프로젝트 정책값은 `aws.s3` prefix에 둡니다.

S3 관련 환경변수는 다음과 같습니다.

| 환경변수 | 사용 위치 | 설명 |
|---|---|---|
| `AWS_REGION` | `spring.cloud.aws.region.static` | S3 리전, 기본값 `ap-northeast-2` |
| `AWS_S3_BUCKET` | `spring.cloud.aws.s3.bucket` | Private S3 Bucket 이름 |
| `AWS_S3_DIRECTORY` | `aws.s3.directory` | 상품 이미지 Object Key prefix, 기본값 `products` |
| `AWS_S3_MAX_FILE_SIZE` | `aws.s3.max-file-size`, multipart limit | 이미지 업로드 최대 크기, 기본값 `5242880` |
| `AWS_ACCESS_KEY_ID` | AWS SDK 기본 자격 증명 체인 | 로컬 개발용 AWS Access Key |
| `AWS_SECRET_ACCESS_KEY` | AWS SDK 기본 자격 증명 체인 | 로컬 개발용 AWS Secret Key |

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
| `/used-market/prod/AWS_S3_MAX_FILE_SIZE` | `aws.s3.max-file-size`, multipart limit | String | N |

현재 단계에서는 빠른 개발과 디버깅을 위해 Spring Boot 컨테이너를 Compose에 포함하지 않습니다. 이후 GitHub Actions나 EC2 배포 단계에서 애플리케이션 컨테이너가 필요해지면 별도 Compose 파일 또는 배포용 프로파일로 확장하는 구조가 적절합니다.

## 배포 브랜치 전략

운영 배포 기준 브랜치: `main`

브랜치 흐름은 다음을 기준으로 합니다.

```text
feature/* → develop PR
develop에서 통합 테스트
운영 테스트 완료 후 develop → main PR
main merge/push 시 운영 배포 실행
```

GitHub Actions는 `develop` 또는 `main` 대상 PR에서는 빌드/테스트만 수행합니다. AWS 인증, ECR Push, EC2 배포는 `main` 브랜치에 merge 또는 push되어 `push` 이벤트가 발생한 경우에만 실행합니다. 따라서 `develop` push만으로는 운영 배포가 실행되지 않습니다.

## 배포 헬스체크

배포 후 EC2, Docker, GitHub Actions에서 서버 생존 여부를 확인할 때 아래 Actuator 엔드포인트를 사용합니다.

```http
GET /actuator/health
```

Actuator는 배포 자동화와 인프라 Health Check가 애플리케이션의 기동 상태를 HTTP로 확인할 수 있도록 추가했습니다. 현재는 보안을 위해 `health` 엔드포인트만 노출하고 상세 정보는 숨깁니다.

`/actuator/health`는 비즈니스 API와 달리 인증, 도메인 데이터, 외부 입력 형식에 의존하지 않는 표준 생존 확인 지점입니다. 따라서 EC2 배포 스크립트, Docker 헬스체크, GitHub Actions 배포 검증에서 서버가 정상적으로 떠 있는지 확인하는 용도로 사용합니다.

Redis 연결 여부도 Health Check에서 확인할 수 있도록 컴포넌트 상태는 노출합니다. 정상 연결 시 응답의 `components.redis.status`가 `UP`으로 표시됩니다.
