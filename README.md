# 5pringUsedMarket
Spring Boot와 JPA를 활용한 중고거래 플랫폼 팀 프로젝트

프로젝트 목표와 AI 협업 규칙은 [`docs/AGENT_RULES.md`](docs/AGENT_RULES.md)를 가장 먼저 확인합니다.

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
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=your-s3-bucket-name
AWS_S3_DIRECTORY=products
AWS_S3_MAX_FILE_SIZE=5242880
AWS_ACCESS_KEY_ID=your-aws-access-key-id
AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
```

MySQL 컨테이너 내부 포트는 `3306`이지만 로컬 PC에는 `3307`로 노출합니다. 로컬에 이미 설치된 MySQL이 `3306`을 사용하는 경우가 많기 때문에 포트 충돌을 피하기 위한 구성입니다.

Redis는 로컬 설치와 충돌 가능성이 비교적 낮고 Spring Boot 기본 Redis 포트도 `6379`이므로 `6379:6379`를 사용합니다.

S3 이미지 업로드는 Spring Cloud AWS S3 starter의 자동 설정을 사용합니다. 로컬에서는 위 예시처럼 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 환경변수로 두고, 배포 환경에서는 IAM Role 또는 Secret Manager 등 외부 주입 방식을 사용합니다.

현재 S3 업로드 로직은 Spring Cloud AWS가 자동 설정한 AWS SDK v2 `S3Client`를 주입받아 사용하는 방식입니다. 별도의 수동 Credentials Bean을 만들지 않으며, Spring Cloud AWS `S3Template`과 혼용하지 않습니다.

Spring Cloud AWS가 사용하는 자격 증명, 리전, 버킷 설정은 `spring.cloud.aws` prefix에 두고, 업로드 디렉터리와 파일 크기 제한 같은 프로젝트 정책값은 `aws.s3` prefix에 둡니다.

Spring Cloud AWS 의존성은 BOM으로 버전을 통합 관리합니다.

```gradle
implementation platform('io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2')
implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3'
```

Parameter Store는 이번 범위에 포함하지 않고, 설정 외부화가 필요해지는 시점에 아래 starter 추가를 검토합니다.

```gradle
implementation 'io.awspring.cloud:spring-cloud-aws-starter-parameter-store'
```

현재 단계에서는 빠른 개발과 디버깅을 위해 Spring Boot 컨테이너를 Compose에 포함하지 않습니다. 이후 GitHub Actions나 EC2 배포 단계에서 애플리케이션 컨테이너가 필요해지면 별도 Compose 파일 또는 배포용 프로파일로 확장하는 구조가 적절합니다.

## 배포 헬스체크

배포 후 EC2, Docker, GitHub Actions에서 서버 생존 여부를 확인할 때 아래 Actuator 엔드포인트를 사용합니다.

```http
GET /actuator/health
```
