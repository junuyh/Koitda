# 코잇다 (Koitda)

도안 탐색·구매 → 작품 계획 → 제작 과정 기록 → 완성 경험 공유를 하나의 흐름으로 잇는 **뜨개 통합 플랫폼**.
핵심 가치는 *다른 사람의 실제 제작 정보(니팅로그)가 다음 사용자의 선택과 시작을 돕는 순환*이다.

## 기술 스택

- **Backend** — Java 21, Spring Boot(Spring Security 세션 인증), Spring Data JPA, PostgreSQL 18, Flyway
- **Frontend** — Next.js, TypeScript, Tailwind CSS, TanStack Query, TipTap 에디터
- **Infra** — Docker Compose(PostgreSQL, MinIO), 이미지·PDF는 객체 저장소(메타데이터·키만 DB)

---

## 로컬 실행 (Quick Start)

### 사전 준비
- Java 21, Node.js, Docker
- macOS 예시. Node/Java 가 `/opt/homebrew/bin` 에 있으면 비대화형 셸 PATH 에 추가.

### 1) 데이터베이스 (+ 객체 저장소)
```bash
cd infra
docker compose up -d postgres        # + minio 필요 시 함께
```
- 기본 포트는 5432. **5432 가 이미 점유돼 있으면** 다른 포트로 띄운다:
  ```bash
  POSTGRES_PORT=5433 docker compose up -d postgres
  ```

### 2) 백엔드 (`http://localhost:8080`)
```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5433/koitda SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```
- `dev` 프로파일이 비어 있는 DB 에 데모 도안·카테고리를 시드한다(DemoDataSeeder).
- Flyway 가 `V1~` 마이그레이션을 순서대로 적용해 스키마를 만든다.
- 카카오 로그인·AI 조언을 켜려면 아래 **환경변수** 참고(선택).

### 3) 프론트엔드 (`http://localhost:3000`)
```bash
cd frontend
BACKEND_ORIGIN=http://localhost:8080 npm run dev
```
- Next `rewrites` 로 `/api/*` 를 백엔드에 프록시한다(동일 출처 → 세션·CSRF 쿠키 정상 동작).

> README 만 보고 로컬 실행이 되도록 유지한다. 비밀값·개인정보는 **저장소·로그에 남기지 않는다.**

---

## 환경변수

키·비밀값은 **커밋하지 않고 환경변수로만** 주입한다. 아래 값들은 모두 **기본값이 있어** 없어도 실행되며, 관련 기능만 비활성화된다.

### 백엔드

| 변수 | 기본값 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/koitda` | DB 접속 URL (포트 5433 로 띄웠으면 그에 맞게) |
| `DB_USERNAME` / `DB_PASSWORD` | `koitda` / `koitda_local_pw` | DB 계정 |
| `SPRING_PROFILES_ACTIVE` | (없음) | `dev` 로 주면 데모 데이터 시드 |
| `STORAGE_DIR` | `storage-data` | 로컬 파일 저장 경로(이미지·PDF 실체) |
| `KAKAO_CLIENT_ID` | (빈값) | 카카오 REST API 키. **없으면 카카오 로그인 버튼 자동 숨김** |
| `KAKAO_CLIENT_SECRET` | (빈값) | (선택) 카카오 Client Secret 을 "사용함" 으로 켰을 때만 필요 |
| `KAKAO_REDIRECT_URI` | `http://localhost:3000/auth/kakao/callback` | 카카오 앱에 등록한 Redirect URI 와 **정확히 일치**해야 함 |
| `GEMINI_API_KEY` | (빈값) | Gemini API 키. **없으면 AI 게이지 조언 버튼 자동 숨김** |
| `GEMINI_MODEL` | `gemini-flash-latest` | (선택) 모델명. 미지원 시 키가 쓸 수 있는 모델을 자동 탐색 |

### 프론트엔드

| 변수 | 기본값 | 설명 |
|---|---|---|
| `BACKEND_ORIGIN` | `http://localhost:8080` | `/api/*` 프록시 대상 백엔드 주소 |

### Docker Compose (`infra/`)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `koitda` / `koitda` / `koitda_local_pw` | PostgreSQL 초기화 |
| `POSTGRES_PORT` | `5432` | 호스트 노출 포트(충돌 시 5433 등으로) |
| `MINIO_API_PORT` / `MINIO_CONSOLE_PORT` | `9000` / `9001` | 객체 저장소 API·콘솔 |

---

## 선택 기능 — 외부 키 연동

두 기능은 **키가 있을 때만 켜지고, 없으면 화면에서 자동으로 숨겨진다.** 이메일 회원가입·로그인 등 나머지는 키 없이 동작한다.

### 카카오 로그인

1. [카카오 개발자센터](https://developers.kakao.com) → 애플리케이션 추가 → **REST API 키** 확인
2. 제품 설정 → 카카오 로그인 **활성화**, **Redirect URI** 에 `http://localhost:3000/auth/kakao/callback` 등록
3. (Web 플랫폼 도메인 `http://localhost:3000` 등록 권장)
4. **보안 → Client Secret** 이 "사용함" 이면 `KAKAO_CLIENT_SECRET` 도 함께 주입(아니면 "사용 안 함")
5. 실행:
   ```bash
   ... KAKAO_CLIENT_ID=<REST_API_키> ./gradlew bootRun
   ```
- 신규 카카오 사용자는 **개인정보 동의** 후 계정이 생성되며, 비밀번호·이메일 없이도 가입된다.

### AI 게이지 조언 (Google Gemini)

1. [Google AI Studio](https://aistudio.google.com/apikey) 에서 **API 키** 발급(무료 티어 있음)
2. 실행:
   ```bash
   ... GEMINI_API_KEY=<Gemini_키> ./gradlew bootRun
   ```
- 게이지 계산 화면에서 **"✨ AI 게이지 조언"** 으로 팁을 받고, 니팅로그에 적용하면 조언이 함께 저장·노출된다.
- **계산·수치는 코드가** 산출하고 AI 는 그 결과 기반 **자연어 조언만** 생성한다(숫자를 새로 만들지 않음).
- 모델은 `gemini-flash-latest` 기본. 구글이 모델을 바꿔 404 가 나면 키가 지원하는 모델로 자동 전환한다.

> **주의**: 키를 소스·채팅·로그에 남기지 않는다. 키가 노출됐다면 발급처에서 폐기·재발급한다.

카카오 키와 Gemini 키를 함께 넣으려면 한 줄로:
```bash
cd backend && DB_URL=jdbc:postgresql://localhost:5433/koitda SPRING_PROFILES_ACTIVE=dev \
  KAKAO_CLIENT_ID=<카카오키> GEMINI_API_KEY=<제미나이키> ./gradlew bootRun
```

---

## 개발/데모 시드

화면 시연용 활동 데이터(니팅로그 집계·베스트셀러 구매)는 수동 시드 스크립트로 채운다.
```bash
docker exec -i koitda-postgres psql -U koitda -d koitda < scripts/dev-seed/demo_activity.sql
```
자세한 내용: [`scripts/dev-seed/README.md`](scripts/dev-seed/README.md)

## 테스트

백엔드는 Testcontainers(자체 PostgreSQL)를 쓰므로 Docker 데몬만 떠 있으면 된다.
```bash
cd backend && ./gradlew test
```

## 기준 문서

구현 범위·계약은 `docs/` 아래 요구사항정의서·ERD·API목록·화면목록을 따른다.
