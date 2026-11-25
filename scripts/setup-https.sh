#!/usr/bin/env bash
# Cài Nginx + chứng chỉ Let's Encrypt trên máy chủ. Chạy từ thư mục gốc repo, trên máy cá nhân:
#
#   SSH_KEY=~/.ssh/khoa.pem bash scripts/setup-https.sh eduadmin@<IP> <tên-miền> <email>
#
# Điều kiện: tên miền đã trỏ (bản ghi A, KHÔNG bật proxy Cloudflare) về IP máy chủ,
# Security Group đã mở cổng 80 và 443.
# Chạy lại nhiều lần được: chứng chỉ đã có thì không xin lại, chỉ cập nhật cấu hình Nginx.
set -euo pipefail

TARGET="${1:?Cách dùng: scripts/setup-https.sh <user@host> <tên-miền> <email>}"
DOMAIN="${2:?Thiếu tên miền}"
EMAIL="${3:?Thiếu email nhận thông báo hết hạn chứng chỉ}"
SSH_OPTS=(-o StrictHostKeyChecking=accept-new)
[ -n "${SSH_KEY:-}" ] && SSH_OPTS+=(-i "$SSH_KEY")

echo ">> Chép cấu hình Nginx lên $TARGET"
scp "${SSH_OPTS[@]}" deploy/nginx/eduzone.conf "$TARGET:/tmp/eduzone.conf"

ssh "${SSH_OPTS[@]}" "$TARGET" sudo bash -s -- "$DOMAIN" "$EMAIL" <<'REMOTE'
set -euo pipefail
DOMAIN="$1"; EMAIL="$2"
LIVE="/etc/letsencrypt/live/$DOMAIN"

echo ">> 1. Cài nginx và certbot"
if ! command -v nginx >/dev/null || ! command -v certbot >/dev/null; then
  apt-get update -y
  apt-get install -y nginx certbot
fi
mkdir -p /var/www/certbot
rm -f /etc/nginx/sites-enabled/default

echo ">> 2. Xin chứng chỉ (nếu chưa có)"
if [ ! -f "$LIVE/fullchain.pem" ]; then
  # Cấu hình tạm chỉ có HTTP để Let's Encrypt xác minh qua /.well-known/acme-challenge/
  cat > /etc/nginx/sites-available/eduzone-bootstrap.conf <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN;
    location /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 404; }
}
EOF
  ln -sf /etc/nginx/sites-available/eduzone-bootstrap.conf /etc/nginx/sites-enabled/eduzone.conf
  nginx -t
  systemctl reload nginx
  certbot certonly --webroot -w /var/www/certbot -d "$DOMAIN" \
    --email "$EMAIL" --agree-tos --no-eff-email --non-interactive
  rm -f /etc/nginx/sites-available/eduzone-bootstrap.conf
else
  echo "   Đã có chứng chỉ tại $LIVE — bỏ qua"
fi

echo ">> 3. Cài cấu hình Nginx chính thức"
sed "s/__DOMAIN__/$DOMAIN/g" /tmp/eduzone.conf > /etc/nginx/sites-available/eduzone.conf
ln -sf /etc/nginx/sites-available/eduzone.conf /etc/nginx/sites-enabled/eduzone.conf
nginx -t
systemctl reload nginx

echo ">> 4. Tự gia hạn: nạp lại Nginx sau mỗi lần certbot gia hạn"
install -d /etc/letsencrypt/renewal-hooks/deploy
printf '#!/bin/sh\nsystemctl reload nginx\n' > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
systemctl list-timers certbot.timer --no-pager || true
certbot renew --dry-run --no-random-sleep-on-renew

echo
echo "== Xong: https://$DOMAIN"
REMOTE
