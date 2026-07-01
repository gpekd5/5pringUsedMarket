# Local Setup

이 문서는 로컬에서 MySQL, Redis, Spring Boot 백엔드, React/Vite 프론트엔드를 실행하는 방법을 정리합니다.

README.md가 다른 PR에서 수정 중일 수 있으므로, 로컬 실행 방법은 우선 이 문서에서 관리합니다.

## 1. 필요한 프로그램

| 프로그램 | 권장 / 확인 버전 | 확인 명령어 |
|---|---:|---|
| Java | 17 | `java -version` |
| Gradle | Wrapper 사용, 현재 `9.5.1` | `.\gradlew.bat --version` |
| Docker Desktop | Docker Engine 20 이상 | `docker --version` |
| Node.js | `^20.19.0` 또는 `>=22.12.0` | `node -v` |
| npm | Node에 포함된 npm 사용, 현재 확인 `11.9.0` | `npm -v` |

현재 프론트는 Vite 8 기반이라 Node.js가 너무 낮으면 실행되지 않습니다.

권장:

```text
Node.js 22 LTS 이상
npm 10 이상
```

현재 개발 PC에서 확인한 버전:

```text
Java 17.0.12
Docker 29.4.3
Node.js v24.14.0
npm 11.9.0
Gradle wrapper 9.5.1
```

## 2. 처음 받은 뒤 한 번만 할 일

프로젝트 루트로 이동합니다.

```powershell
cd C:\Users\gpekd\fivespring-used-market
```

`src/main/resources/application-local.yml`은 개인 로컬 설정이라 Git에 올라가지 않습니다.

아래 템플릿을 복사해서 로컬 설정 파일을 만듭니다.

```powershell
Copy-Item .\src\main\resources\application-local.yml.template .\src\main\resources\application-local.yml
```

기본 Docker Compose 설정을 그대로 쓰면 템플릿 기본값으로 실행할 수 있습니다.

주요 기본값:

| 항목 | 값 |
|---|---|
| MySQL URL | `jdbc:mysql://localhost:3307/fivespring_used_market` |
| MySQL username | `root` |
| MySQL password | `12345678` |
| Redis host | `localhost` |
| Redis port | `6379` |
| Backend URL | `http://localhost:8080` |
| Frontend URL | `http://localhost:5173` |

## 3. MySQL, Redis 실행

프로젝트 루트에서 실행합니다.

```powershell
docker compose up -d mysql redis
```

실행 상태 확인:

```powershell
docker compose ps
```

MySQL은 로컬 PC에서 `3307` 포트로 접근합니다.

Redis는 검색 캐시, 인기검색어, 토큰 블랙리스트, 채팅 Pub/Sub에서 사용하므로 백엔드 실행 전에 같이 켜야 합니다.

## 4. 백엔드 실행

프로젝트 루트에서 실행합니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

백엔드 실행 주소:

```text
http://localhost:8080
```

헬스체크:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

브라우저에서 확인:

```text
http://localhost:8080/actuator/health
```

`local` 프로필에서는 아래 계정이 자동 생성됩니다.

| 역할 | 이메일 | 비밀번호 |
|---|---|---|
| 일반 회원 | `member@test.com` | `Password123!` |
| 관리자 | `admin@test.com` | `Password123!` |

기본 데모 쿠폰도 자동 생성됩니다.

## 5. 프론트엔드 실행

새 PowerShell 창을 열고 프로젝트 루트에서 실행합니다.

```powershell
cd frontend
npm install
npm run dev
```

프론트엔드 실행 주소:

```text
http://localhost:5173
```

프론트 API 주소는 `frontend/.env`에서 관리합니다.

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

의존성을 lock 파일 기준으로 더 엄격하게 설치하고 싶으면 `npm install` 대신 아래 명령어를 사용할 수 있습니다.

```powershell
npm ci
```

프론트 빌드 확인:

```powershell
npm run build
```

## 6. 전체 실행 순서 요약

터미널 1: MySQL, Redis

```powershell
docker compose up -d mysql redis
```

터미널 2: 백엔드

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

터미널 3: 프론트엔드

```powershell
cd frontend
npm install
npm run dev
```

브라우저 접속:

```text
http://localhost:5173
```

## 7. 시연용 SQL 데이터

상품, 찜, 채팅, 쿠폰까지 한 번에 채운 시연용 데이터가 필요하면 백엔드를 한 번 실행해 테이블과 기본 계정을 만든 뒤 아래 명령을 실행합니다.

프로젝트 루트에서 실행합니다.

```powershell
Get-Content -Encoding UTF8 -Raw .\src\main\resources\db\demo-data.sql | docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -p12345678 fivespring_used_market
```

이 SQL은 `[DEMO]` 데이터만 지우고 다시 넣습니다.

서버 시작 시 자동 실행되는 파일이 아니며, 발표나 테스트 전에 필요할 때만 수동 실행합니다.

## 8. 대용량 더미 데이터

일반 `local` 실행에서는 대용량 더미 데이터가 생성되지 않습니다.

성능 테스트용 대용량 데이터가 필요할 때만 `bulk-dummy` 프로필을 함께 활성화합니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local,bulk-dummy'
```

## 9. 종료 명령어

프론트/백엔드는 실행 중인 터미널에서 `Ctrl + C`로 종료합니다.

MySQL, Redis 컨테이너 종료:

```powershell
docker compose down
```

볼륨까지 삭제해서 DB 데이터를 완전히 초기화하고 싶을 때:

```powershell
docker compose down -v
```

`down -v`는 MySQL 데이터까지 삭제하므로 정말 초기화가 필요할 때만 사용합니다.

## 10. 자주 확인하는 명령어

현재 Git 변경 상태:

```powershell
git status --short
```

백엔드 테스트:

```powershell
.\gradlew.bat test
```

프론트 빌드:

```powershell
cd frontend
npm run build
```

Docker 컨테이너 로그:

```powershell
docker compose logs mysql
docker compose logs redis
```

