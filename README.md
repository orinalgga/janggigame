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

### VS Code에서

1. Java 21(JDK)과 Node 22를 설치하고 저장소 폴더를 VS Code로 엽니다.
2. 오른쪽 아래에 뜨는 추천 확장(Java, Spring Boot, Gradle, Vue)을 설치합니다. Java 프로젝트를 불러오는 데 처음엔 1~2분 걸립니다.
3. 터미널 → 작업 실행 → `웹 의존성 설치`를 한 번 돌립니다.
4. 실행 및 디버그(Ctrl+Shift+D)에서 `서버 + 웹`을 고르고 F5를 누르면 서버와 웹이 함께 뜨고 Chrome이 열립니다. 서버 코드에 중단점을 걸 수 있습니다.
5. 구글 로그인까지 쓰려면 `.env.example`을 `.env`로 복사해 키를 채우고 `서버 (구글 로그인) + 웹`으로 실행합니다. 온라인 대전은 로그인한 계정 두 개(일반 창과 시크릿 창)로 시험합니다.

전체 빌드와 테스트는 Ctrl+Shift+B가 아니라 터미널 → 작업 실행 → `전체 빌드와 테스트`, 또는 명령 팔레트의 `Tasks: Run Test Task`로 돌립니다.

로컬에서는 메모리 DB(H2)를 쓰므로 서버를 끄면 기보가 사라집니다. Postgres에 남기려면 `DB_URL`, `DB_USER`, `DB_PASSWORD` 환경변수를 넣고 실행합니다. 스키마는 Flyway(`server/src/main/resources/db/migration`)가 만듭니다.

## 규칙 엔진 좌표

- x는 0~8(세로줄), y는 0~9(가로줄)이고 초가 y=0 쪽, 한이 y=9 쪽입니다.
- 초가 먼저 둡니다. 한은 덤 1.5점을 받아 시작 점수가 초 72, 한 73.5입니다.
- 상차림은 각 진영이 자기 쪽에서 본 왼쪽부터 정합니다.
- 장군이 아닐 때는 한수쉼(패스)을 둘 수 있습니다. 빅장(두 장이 사이에 아무것도 없이 마주 봄)이 되면 바로 점수 판정으로 끝나고 점수가 높은 쪽이 이깁니다. 한의 덤 1.5점 때문에 동점은 나오지 않습니다. 외통과 빅장이 동시에 생기면 외통이 우선입니다.

## 로그인

구글 로그인은 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 환경변수가 있을 때만 켜지고, 없으면 로그인 없이 그대로 쓸 수 있습니다. 키는 저장소나 채팅에 올리지 않습니다.

```bash
GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... ./gradlew :server:bootRun
```

- Google Cloud 콘솔의 OAuth 클라이언트에 승인된 리디렉션 URI `http://localhost:8080/login/oauth2/code/google`(배포 후에는 `https://<서버 주소>/login/oauth2/code/google`)를 등록합니다.
- 로그인이 끝나면 서버가 `JANGGI_WEB_BASE_URL/#token=...`으로 돌려보내고, 웹은 이 토큰을 `Authorization: Bearer`로 보냅니다.
- 운영에서는 `JANGGI_JWT_SECRET`에 32자 이상 임의 문자열을 꼭 넣습니다. docker compose는 이 값이 없으면 시작하지 않습니다.

## AI

`janggi-core`의 `Engine`이 반복 심화 알파베타 탐색으로 수를 고릅니다. 잡는 수는 끝까지 더 읽고(정지 탐색), 빅장은 점수 판정 결과로 평가합니다.

| 난이도 | 깊이 | 생각 시간 | 실수 |
| --- | --- | --- | --- |
| 초급 | 1 | 0.3초 | 30% 확률로 아무 수, 2.5점 차 이내 수 중 무작위 |
| 중급 | 2 | 1초 | 10% 확률로 아무 수, 0.8점 차 이내 수 중 무작위 |
| 상급 | 최대 5 | 2초 | 없음 |

## 온라인 대전

로그인한 회원끼리 둡니다. 상대 찾기를 누르면 먼저 기다리던 회원과 바로 짝지어지고, 진영(초/한)은 무작위입니다.

- 수는 REST(`POST /api/games/{id}/moves`)로 두고, 서버가 바뀐 국면을 STOMP `/topic/games/{id}`로 두 사람에게 보냅니다. 매칭 결과는 `/user/queue/match`로 옵니다. 웹소켓 주소는 `/ws`, 접속할 때 `Authorization: Bearer` 헤더로 회원을 확인합니다.
- 시간 규칙은 피셔 방식입니다. 기본 10분에 한 수마다 10초가 더해지고(`JANGGI_ONLINE_BASE_SECONDS`, `JANGGI_ONLINE_INCREMENT_SECONDS`), 시간이 다 되면 아무도 두지 않아도 시간패로 끝납니다.
- 기권(`POST /api/games/{id}/resign`)이 있고, 끝나면 일로 레이팅(K=32)이 바뀝니다. 처음 레이팅은 1200입니다.
- 서버가 다시 켜지면 진행 중이던 온라인 대국을 기보와 남은 시간으로 되살립니다. 꺼져 있던 시간은 둘 차례였던 쪽이 쓴 것으로 칩니다.
- 대기열은 서버 메모리에 있습니다. 서버를 여러 대로 늘릴 때 Redis로 옮깁니다.

## 채팅

대국 화면 옆에 채팅 창이 있습니다. 같은 화면 2인 대국에는 없습니다. 채팅은 서버 메모리에만 두어 서버가 다시 켜지면 사라지고, 한 번에 200자까지 보낼 수 있습니다.

- 온라인 대전: 두 참가자만 보낼 수 있고(`POST /api/games/{id}/chat`), 서버가 STOMP `/topic/games/{id}/chat`으로 두 사람에게 보냅니다.
- AI 대국: 난이도마다 캐릭터가 있습니다(초급 새내기 장돌이, 중급 동네 고수 박씨, 상급 장기 도사). 대국을 시작하면 먼저 인사하고, 장군·큰 알을 잡거나 잃을 때·대국이 끝날 때 한마디 합니다(수 응답의 `aiComment`). 이 한마디는 정해 둔 말이라 비용이 들지 않습니다.
- AI에게 말을 걸면, 서버에 `ANTHROPIC_API_KEY`가 있을 때는 Claude(`JANGGI_AI_CHAT_MODEL`, 기본 `claude-opus-5-5`)가 대국 상황을 보고 캐릭터 말투로 답합니다. 키가 없거나 호출이 실패하면 정해 둔 말로 답합니다. 한 대국에서 Claude에게 묻는 횟수는 `JANGGI_AI_CHAT_MAX_REPLIES`(기본 40)까지입니다.

## 배포 (무료 구성)

- 서버: Oracle Cloud Always Free VM에서 `docker compose up -d --build`
- 웹소켓(`/ws`)도 같은 서버 주소로 붙으므로, 앞단 프록시(Cloudflare 등)에서 웹소켓을 허용해야 합니다.
- 웹: Cloudflare Pages에 `web` 폴더를 연결, 빌드 명령 `npm run build`, 출력 폴더 `dist`
- 웹과 서버의 주소가 다르면 서버에 `JANGGI_CORS_ALLOWED_ORIGINS`, 웹에 `VITE_API_BASE`를 설정합니다.

## 로드맵

0단계 기반 → 1단계 웹 MVP(AI 대전, 회원, 기보) → 2단계 온라인 대전 → 3단계 앱과 상점 → 4단계 대회 모드
