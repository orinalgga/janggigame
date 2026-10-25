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

### Windows에서 한 번에 켜기

`run-dev.cmd`를 더블클릭하면 최신 코드를 받고(`git pull`), 웹 의존성을 확인한 뒤 서버 창과 웹 창을 따로 띄웁니다. `.env`가 있으면 그 값을 화면에 찍지 않고 서버 환경변수로 넣습니다. 처음 연 창을 열어 두면 1분마다 새 코드를 받아, 웹은 바로 바뀌고 서버 코드가 바뀌었으면 서버 창을 다시 띄웁니다.

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

## 급수

레이팅에 따라 급수가 저절로 오르내린다(`user/Grade.java`). 800점 아래가 18급이고 50점마다 한 칸씩 올라 1600점이 1급, 1650점이 초단, 2050점부터 9단이다. 처음 레이팅 1200점은 9급. 온라인 대국이 끝나 급수가 바뀌면 화면 위에 승급·강등 알림이 뜬다.

## 채팅

대국 화면 옆에 채팅 창이 있습니다. 같은 화면 2인 대국에는 없습니다. 채팅은 서버 메모리에만 두어 서버가 다시 켜지면 사라지고, 한 번에 200자까지 보낼 수 있습니다.

- 온라인 대전: 두 참가자만 보낼 수 있고(`POST /api/games/{id}/chat`), 서버가 STOMP `/topic/games/{id}/chat`으로 두 사람에게 보냅니다.
- AI 대국: 난이도마다 캐릭터가 있습니다(초급 새내기 장돌이, 중급 동네 고수 박씨, 상급 장기 도사). 대국을 시작하면 먼저 인사하고, 장군·큰 알을 잡거나 잃을 때·대국이 끝날 때 한마디 합니다(수 응답의 `aiComment`). 이 한마디는 정해 둔 말이라 비용이 들지 않습니다.
- AI에게 말을 걸면, 서버에 `ANTHROPIC_API_KEY`가 있을 때는 Claude(`JANGGI_AI_CHAT_MODEL`, 기본 `claude-opus-5-5`)가 대국 상황을 보고 캐릭터 말투로 답합니다. 키가 없거나 호출이 실패하면 정해 둔 말로 답합니다. 한 대국에서 Claude에게 묻는 횟수는 `JANGGI_AI_CHAT_MAX_REPLIES`(기본 40)까지입니다.

## 상점

장기판 테마 5종과 장기알 테마 5종이 있고, 각각 하나씩 무료 기본 테마가 있다. 상품 목록은 `server/.../shop/ShopItem.java`에, 색은 같은 id로 `web/src/themes.ts`에 정의한다. 테마는 장기판의 CSS 변수만 바꾼다.

- 누구나 상점을 열고 미리 볼 수 있다. 사기와 장착은 로그인해야 하고, 장착한 테마는 회원 정보에 저장된다.
- 결제 흐름: `POST /api/shop/orders`로 주문을 만들고, 결제창에서 받은 키로 `POST /api/shop/orders/{orderId}/confirm`을 부르면 서버가 결제사에 승인받은 뒤 상품을 넣어 준다. 금액은 서버의 주문 금액을 쓴다.
- 지금 결제사는 돈이 오가지 않는 `test`뿐이다(`JANGGI_PAYMENTS_PROVIDER`). 실제 결제사(토스페이먼츠 등)는 `PaymentGateway`를 구현해 붙인다.

## 관리자

`.env`의 `JANGGI_ADMIN_EMAILS`에 구글 메일을 적고(쉼표로 여러 개) 그 메일로 로그인하면 로비 오른쪽 위에 **관리자** 버튼이 생긴다.

- 현황: 전체·오늘 가입자, 오늘 대국 수, 진행 중인 온라인 대국과 상대를 찾는 사람 수, 매출(오늘은 한국 시간 기준).
- 회원: 닉네임·메일·회원 번호로 검색, 닉네임 강제 변경, 테마 지급, 이용 정지와 해제. 정지된 회원은 자기 정보만 볼 수 있고 대국·채팅·상점은 막힌다. 관리자는 정지할 수 없다.
- 주문: 최근 100건.
- 기록: 관리자가 한 닉네임 변경·정지·해제·지급을 누가 언제 했는지 최근 100건.
- 구글이 확인한 메일(`email_verified`)만 저장하므로, 확인 안 된 메일로는 관리자가 될 수 없다. 관리자 권한은 매 요청마다 서버에서 다시 확인한다.
- 운영 DB(H2가 아닌 DB)에 붙었는데 `JANGGI_JWT_SECRET`이 비었거나 개발용 값이면 서버가 켜지지 않는다.

## 모바일 앱

웹 화면을 Capacitor로 감싸 안드로이드·iOS 앱으로 만든다(`web/android`, `web/ios`). 앱 id는 `com.janggi.app`.

1. 앱이 붙을 서버 주소를 `web/.env.app.local`에 적는다(이 파일은 저장소에 올라가지 않는다).
   - 집에서 시험할 때: `VITE_API_BASE=http://<PC의 내부 IP>:8080` (휴대폰과 PC가 같은 와이파이, Windows 방화벽에서 8080 허용)
   - 배포 후: `VITE_API_BASE=https://<서버 주소>`
2. 안드로이드: [Android Studio](https://developer.android.com/studio)를 설치하고 `cd web && npm run app:android`. Android Studio가 열리면 휴대폰을 USB로 연결(개발자 옵션의 USB 디버깅 켜기)하고 ▶ 실행.
3. iOS: Mac과 Xcode가 필요하다. `npm run app:ios`.

웹을 고친 뒤에는 `npm run app:build`로 다시 넣는다.

- 구글은 앱 안 화면에서 로그인을 막으므로, 앱은 기기 브라우저로 로그인하고 `com.janggi.app://auth#token=...`으로 돌아온다(`JANGGI_APP_REDIRECT_URL`). 구글 로그인은 구글에 등록한 서버 주소에서만 되므로, 내부 IP로 시험할 때는 로그인 없이 AI 대국·같은 화면 대국만 된다.
- 서버의 `JANGGI_CORS_ALLOWED_ORIGINS`에는 앱 화면 주소 `https://localhost`(안드로이드), `capacitor://localhost`(iOS)가 들어 있어야 한다(기본값에 포함).
- 앱 스토어에 올리면 테마 같은 디지털 상품은 스토어 인앱 결제를 써야 한다. 지금 상점의 테스트 결제는 출시 전에 바꾼다.

## 배포 (무료 구성)

- 서버: Oracle Cloud Always Free VM에서 `docker compose up -d --build`
- 웹소켓(`/ws`)도 같은 서버 주소로 붙으므로, 앞단 프록시(Cloudflare 등)에서 웹소켓을 허용해야 합니다.
- 웹: Cloudflare Pages에 `web` 폴더를 연결, 빌드 명령 `npm run build`, 출력 폴더 `dist`
- 웹과 서버의 주소가 다르면 서버에 `JANGGI_CORS_ALLOWED_ORIGINS`, 웹에 `VITE_API_BASE`를 설정합니다.

## 로드맵

0단계 기반 → 1단계 웹 MVP(AI 대전, 회원, 기보) → 2단계 온라인 대전 → 3단계 앱과 상점 → 4단계 대회 모드
