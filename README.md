# 장기 (Janggi)

웹과 모바일에서 AI 대전, 온라인 대전, 정해진 시간의 대회를 즐기는 한국 장기 서비스입니다.

## 구성

| 폴더 | 내용 |
| --- | --- |
| `janggi-core` | 순수 Java 장기 규칙 엔진. 서버와 AI가 함께 쓴다 |
| `server` | Spring Boot 3 서버 (REST, WebSocket) |
| `web` | Vue 3 + TypeScript + Vite 웹 클라이언트 |

## 로컬에서 실행

필요한 것: Java 21, Node 22

```bash
# 서버 (http://localhost:8080)
./gradlew :server:bootRun

# 웹 (http://localhost:5173, /api 요청은 서버로 넘어감)
cd web
npm install
npm run dev
```

테스트는 `./gradlew build`로 돌립니다.

로컬에서는 메모리 DB(H2)를 쓰므로 서버를 끄면 기보가 사라집니다. Postgres에 남기려면 `DB_URL`, `DB_USER`, `DB_PASSWORD` 환경변수를 넣고 실행합니다. 스키마는 Flyway(`server/src/main/resources/db/migration`)가 만듭니다.

## 규칙 엔진 좌표

- x는 0~8(세로줄), y는 0~9(가로줄)이고 초가 y=0 쪽, 한이 y=9 쪽입니다.
- 초가 먼저 둡니다. 한은 덤 1.5점을 받아 시작 점수가 초 72, 한 73.5입니다.
- 상차림은 각 진영이 자기 쪽에서 본 왼쪽부터 정합니다.
- 장군이 아닐 때는 한수쉼(패스)을 둘 수 있습니다. 빅장(두 장이 사이에 아무것도 없이 마주 봄)이 되면 바로 점수 판정으로 끝나고 점수가 높은 쪽이 이깁니다. 한의 덤 1.5점 때문에 동점은 나오지 않습니다. 외통과 빅장이 동시에 생기면 외통이 우선입니다.

## AI

`janggi-core`의 `Engine`이 반복 심화 알파베타 탐색으로 수를 고릅니다. 잡는 수는 끝까지 더 읽고(정지 탐색), 빅장은 점수 판정 결과로 평가합니다.

| 난이도 | 깊이 | 생각 시간 | 실수 |
| --- | --- | --- | --- |
| 초급 | 1 | 0.3초 | 30% 확률로 아무 수, 2.5점 차 이내 수 중 무작위 |
| 중급 | 2 | 1초 | 10% 확률로 아무 수, 0.8점 차 이내 수 중 무작위 |
| 상급 | 최대 5 | 2초 | 없음 |

## 배포 (무료 구성)

- 서버: Oracle Cloud Always Free VM에서 `docker compose up -d --build`
- 웹: Cloudflare Pages에 `web` 폴더를 연결, 빌드 명령 `npm run build`, 출력 폴더 `dist`
- 웹과 서버의 주소가 다르면 서버에 `JANGGI_CORS_ALLOWED_ORIGINS`, 웹에 `VITE_API_BASE`를 설정합니다.

## 로드맵

0단계 기반 → 1단계 웹 MVP(AI 대전, 회원, 기보) → 2단계 온라인 대전 → 3단계 앱과 상점 → 4단계 대회 모드
