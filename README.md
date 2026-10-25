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

## 규칙 엔진 좌표

- x는 0~8(세로줄), y는 0~9(가로줄)이고 초가 y=0 쪽, 한이 y=9 쪽입니다.
- 초가 먼저 둡니다. 한은 덤 1.5점을 받아 시작 점수가 초 72, 한 73.5입니다.
- 상차림은 각 진영이 자기 쪽에서 본 왼쪽부터 정합니다.
- 장군이 아닐 때는 한수쉼(패스)을 둘 수 있습니다. 빅장은 감지만 하고, 판정 방식은 아직 정하지 않았습니다.

## 배포 (무료 구성)

- 서버: Oracle Cloud Always Free VM에서 `docker compose up -d --build`
- 웹: Cloudflare Pages에 `web` 폴더를 연결, 빌드 명령 `npm run build`, 출력 폴더 `dist`
- 웹과 서버의 주소가 다르면 서버에 `JANGGI_CORS_ALLOWED_ORIGINS`, 웹에 `VITE_API_BASE`를 설정합니다.

## 로드맵

0단계 기반 → 1단계 웹 MVP(AI 대전, 회원, 기보) → 2단계 온라인 대전 → 3단계 앱과 상점 → 4단계 대회 모드
