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
```

MySQL 컨테이너 내부 포트는 `3306`이지만 로컬 PC에는 `3307`로 노출합니다. 로컬에 이미 설치된 MySQL이 `3306`을 사용하는 경우가 많기 때문에 포트 충돌을 피하기 위한 구성입니다.

Redis는 로컬 설치와 충돌 가능성이 비교적 낮고 Spring Boot 기본 Redis 포트도 `6379`이므로 `6379:6379`를 사용합니다.

현재 단계에서는 빠른 개발과 디버깅을 위해 Spring Boot 컨테이너를 Compose에 포함하지 않습니다. 이후 GitHub Actions나 EC2 배포 단계에서 애플리케이션 컨테이너가 필요해지면 별도 Compose 파일 또는 배포용 프로파일로 확장하는 구조가 적절합니다.
