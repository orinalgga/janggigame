#!/usr/bin/env bash
# Oracle Cloud Ubuntu VM에서 한 번만 실행한다: bash deploy/setup-vm.sh
set -euo pipefail

# 도커 설치
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

# Oracle Ubuntu 이미지는 VM 안의 방화벽(iptables)이 80·443을 막아 둔다.
for port in 443 80; do
  if ! sudo iptables -C INPUT -p tcp --dport "$port" -m state --state NEW -j ACCEPT 2>/dev/null; then
    sudo iptables -I INPUT 6 -p tcp --dport "$port" -m state --state NEW -j ACCEPT
  fi
done
if command -v netfilter-persistent >/dev/null; then
  sudo netfilter-persistent save
fi

# 메모리가 4GB보다 적으면 서버 빌드가 멈추지 않게 스왑을 만든다.
mem_kb=$(awk '/MemTotal/ {print $2}' /proc/meminfo)
if [ "$mem_kb" -lt 4000000 ] && [ ! -f /swapfile ]; then
  sudo fallocate -l 4G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

echo "끝났습니다. 도커 권한을 적용하려면 한 번 로그아웃했다가 다시 접속하세요."
