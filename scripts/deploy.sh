#!/usr/bin/env bash
# Triển khai EduZone lên máy chủ bằng image KÉO TỪ ghcr.io (không build trên máy chủ).
# Chạy từ thư mục gốc repo, trên máy cá nhân:
#
#   SSH_KEY=~/.ssh/eduzone.pem bash scripts/deploy.sh eduadmin@<IP> [tag]
#
# tag mặc định = 7 ký tự đầu của commit HEAD (phải trùng tag CI đã đẩy lên registry).
set -euo pipefail

TARGET="${1:?Cách dùng: scripts/deploy.sh <user@host> [tag]}"
TAG="${2:-$(git rev-parse HEAD | cut -c1-7)}"
ENV_FILE="${ENV_FILE:-deploy/.env.prod}"
SSH_OPTS=(-o StrictHostKeyChecking=accept-new)
[ -n "${SSH_KEY:-}" ] && SSH_OPTS+=(-i "$SSH_KEY")

[ -f "$ENV_FILE" ] || { echo "Thiếu $ENV_FILE (sao chép từ deploy/.env.prod.example)"; exit 1; }

echo ">> Triển khai tag $TAG lên $TARGET"
ssh "${SSH_OPTS[@]}" "$TARGET" "mkdir -p ~/eduzone/postgres/init"
scp "${SSH_OPTS[@]}" deploy/docker-compose.prod.yml "$TARGET:eduzone/docker-compose.yml"
scp "${SSH_OPTS[@]}" deploy/postgres/init/01-create-databases.sh "$TARGET:eduzone/postgres/init/"
scp "${SSH_OPTS[@]}" "$ENV_FILE" "$TARGET:eduzone/.env"

ssh "${SSH_OPTS[@]}" "$TARGET" bash -s -- "$TAG" <<'REMOTE'
set -euo pipefail
TAG="$1"
cd ~/eduzone
sed -i 's/\r$//' .env postgres/init/*.sh                 # bỏ CRLF nếu file soạn trên Windows
if grep -q '^IMAGE_TAG=' .env; then sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$TAG/" .env; else echo "IMAGE_TAG=$TAG" >> .env; fi
docker compose pull
docker compose up -d --wait --wait-timeout 300
docker compose ps --format 'table {{.Name}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
REMOTE
