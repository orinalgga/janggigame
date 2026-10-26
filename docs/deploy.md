# 서버 배포 안내 (무료 구성)

- 서버(Spring Boot + Postgres + Redis): Oracle Cloud Always Free VM 한 대, Docker Compose
- HTTPS: 같은 VM의 caddy가 Let's Encrypt 인증서를 자동으로 받는다
- 웹(Vue): Cloudflare Pages

도메인이 없어도 된다. 서버 주소는 `<공인IP>.sslip.io`를 쓰고(예: `140.238.1.2.sslip.io`), 웹은 `<프로젝트이름>.pages.dev`를 쓴다. 나중에 도메인을 사면 두 주소만 바꾸면 된다.

## 1. Oracle VM 만들기

빠른 방법: VM 만들기 화면의 Advanced options → Initialization script → Paste cloud-init script에 `deploy/cloud-init.sh` 내용을 붙여 넣으면 2단계(도커 설치, .env, 서버 실행)를 VM이 스스로 한다. 구글 키 등은 나중에 `.env`에 채운다.

1. Oracle Cloud 콘솔 → Compute → Instances → Create instance
2. Image: **Canonical Ubuntu 22.04** (또는 24.04)
3. Shape: **Ampere VM.Standard.A1.Flex** (Always Free, 2 OCPU / 12GB 정도면 충분)
   - "Out of capacity"가 나오면 OCPU를 1로 줄이거나 다른 시간에 다시 시도한다.
   - A1이 끝내 안 되면 VM.Standard.E2.1.Micro(1GB, AMD)도 된다. 이때는 `.env`에 `JAVA_TOOL_OPTIONS=-Xmx384m`을 넣는다.
4. SSH 키: "Generate a key pair"로 개인 키를 내려받아 둔다.
5. 만든 뒤 **Public IP address**를 적어 둔다.

### 80·443 포트 열기 (VCN)

Instance 상세 → Primary VNIC의 Subnet → Security List → Add Ingress Rules:

| Source CIDR | Protocol | Destination Port |
|---|---|---|
| 0.0.0.0/0 | TCP | 80 |
| 0.0.0.0/0 | TCP | 443 |

(VM 안의 방화벽은 아래 `setup-vm.sh`가 연다.)

## 2. VM에 서버 올리기

내 PC의 PowerShell에서 접속한다.

```powershell
ssh -i <내려받은 키 파일> ubuntu@<공인IP>
```

VM 안에서:

```bash
git clone https://github.com/orinalgga/janggigame.git
cd janggigame
bash deploy/setup-vm.sh
exit   # 도커 권한 적용을 위해 한 번 나갔다가 다시 접속
```

다시 접속해서 `.env`를 만든다.

```bash
cd janggigame
cp .env.example .env
nano .env
```

채울 값(비밀값은 채팅이나 저장소에 올리지 않는다):

| 이름 | 값 |
|---|---|
| `COMPOSE_PROFILES` | `prod` |
| `JANGGI_API_HOST` | `<공인IP>.sslip.io` |
| `JANGGI_WEB_BASE_URL` | `https://<프로젝트이름>.pages.dev` (3단계에서 정함) |
| `JANGGI_CORS_ALLOWED_ORIGINS` | `https://<프로젝트이름>.pages.dev,https://localhost,capacitor://localhost` |
| `JANGGI_JWT_SECRET` | 임의 문자열 32자 이상. `openssl rand -base64 48`로 만든다 |
| `POSTGRES_PASSWORD` | 임의 문자열. 처음 띄우기 전에 정한다 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | 구글 콘솔 값 |
| `JANGGI_ADMIN_EMAILS` | 관리자 구글 메일 |
| `ANTHROPIC_API_KEY` | (선택) AI 채팅용 |

띄운다. 서버 이미지는 GitHub Actions(`Server image`)가 main마다 만들어 `ghcr.io/orinalgga/janggigame-server`에 올려 두므로 VM에서 빌드하지 않는다.

```bash
docker compose pull
docker compose up -d --no-build
docker compose logs -f server   # Started ... 이 보이면 Ctrl+C
```

확인: 브라우저에서 `https://<공인IP>.sslip.io/actuator/health` → `{"status":"UP"}`

## 3. Cloudflare Pages에 웹 올리기

1. Cloudflare 대시보드 → Workers & Pages → Create → Pages → Connect to Git → `orinalgga/janggigame`
2. 설정:
   - Framework preset: Vue (또는 None)
   - Build command: `npm run build`
   - Build output directory: `dist`
   - Root directory: `web`
   - Environment variables:
     - `VITE_API_BASE` = `https://<공인IP>.sslip.io`
     - `NODE_VERSION` = `22`
3. 배포가 끝나면 `https://<프로젝트이름>.pages.dev` 주소가 나온다. 이 주소가 2단계 `.env`의 값과 다르면 `.env`를 고치고 VM에서 `docker compose up -d`.

main에 머지할 때마다 Pages는 자동으로 다시 배포된다.

## 4. 구글 로그인 주소 등록

Google Cloud 콘솔 → API 및 서비스 → 사용자 인증 정보 → OAuth 클라이언트:

- 승인된 리디렉션 URI에 추가: `https://<공인IP>.sslip.io/login/oauth2/code/google`
- 승인된 JavaScript 원본에 추가: `https://<프로젝트이름>.pages.dev`

OAuth 동의 화면이 "테스트" 상태면 테스트 사용자로 등록한 계정만 로그인된다. 누구나 쓰게 하려면 "프로덕션으로 게시"한다.

## 업데이트

```bash
cd janggigame
bash deploy/update.sh
```

## 알아 둘 것

- 서버 이미지(ghcr.io 패키지)는 공개(Public)여야 VM이 로그인 없이 받는다. GitHub → 프로필 → Packages → janggigame-server → Package settings → Change visibility.

- 결제는 아직 테스트 결제사(`JANGGI_PAYMENTS_PROVIDER=test`)라서 엽전 충전에 돈이 들지 않는다. 정식 출시 전에 실제 결제사를 붙이고, 그동안 쌓인 엽전은 정리한다.
- DB는 도커 볼륨(`pgdata`)에 있다. 백업: `docker compose exec postgres pg_dump -U janggi janggi > backup.sql`
- 서버 로그: `docker compose logs --tail 200 server`
