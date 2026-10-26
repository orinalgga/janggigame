#!/bin/bash
# Oracle VM을 만들 때 "Initialization script(cloud-init)"에 붙여 넣는 스크립트.
# VM이 처음 켜질 때 도커 설치, 방화벽 열기, 저장소 받기, .env 만들기, 서버 실행까지 한다.
# 비밀값(JWT, DB 비밀번호)은 VM 안에서 무작위로 만들어 어디에도 보내지 않는다.
# 구글 로그인 키 등은 나중에 VM의 ~/janggigame/.env에 직접 채운 뒤 docker compose up -d.
# Pages 주소가 janggi.pages.dev가 아니면 아래 두 줄을 고친다.
set -eux
WEB=https://janggi.pages.dev

# 메모리가 4GB보다 적으면 스왑을 만든다.
if [ "$(awk '/MemTotal/ {print $2}' /proc/meminfo)" -lt 4000000 ] && [ ! -f /swapfile ]; then
  fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

curl -fsSL https://get.docker.com | sh
usermod -aG docker ubuntu
# Oracle Ubuntu 이미지는 VM 안의 방화벽이 80·443을 막아 둔다.
for port in 443 80; do iptables -I INPUT 6 -p tcp --dport $port -m state --state NEW -j ACCEPT; done
netfilter-persistent save || true

cd /home/ubuntu
sudo -u ubuntu git clone https://github.com/orinalgga/janggigame.git
cd janggigame
IP=$(curl -fsS https://ifconfig.me)
cat > .env <<EOF
COMPOSE_PROFILES=prod
JANGGI_API_HOST=$IP.sslip.io
JANGGI_WEB_BASE_URL=$WEB
JANGGI_CORS_ALLOWED_ORIGINS=$WEB,https://localhost,capacitor://localhost
JANGGI_JWT_SECRET=$(openssl rand -hex 32)
POSTGRES_PASSWORD=$(openssl rand -hex 24)
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
JANGGI_ADMIN_EMAILS=
ANTHROPIC_API_KEY=
EOF
chown ubuntu:ubuntu .env
chmod 600 .env

# 서버 이미지가 아직 공개되지 않았을 수 있으니 받을 때까지 1시간 동안 다시 시도한다.
for i in $(seq 1 120); do docker compose pull && break; sleep 30; done
docker compose up -d --no-build
