#!/usr/bin/env bash
# Khởi tạo máy chủ Ubuntu 24.04 cho EduZone. Chạy MỘT lần, bằng quyền root, từ máy cá nhân:
#
#   ssh -i <khoa.pem> ubuntu@<IP> 'sudo bash -s -- eduadmin' < scripts/server-bootstrap.sh
#
# Việc làm:
#   1. Tạo người dùng quản trị riêng (mặc định: eduadmin), sudo không mật khẩu,
#      dùng lại khoá SSH đang đăng nhập của user ubuntu.
#   2. Khoá SSH: tắt đăng nhập bằng mật khẩu, cấm root đăng nhập.
#   3. Cài Docker Engine + Compose plugin từ kho chính thức của Docker.
#   4. Tạo swap 2 GB nếu máy chưa có (đỡ cho Elasticsearch ở ngày 4).
set -euo pipefail

ADMIN_USER="${1:-eduadmin}"
SOURCE_USER="${SOURCE_USER:-ubuntu}"

[ "$(id -u)" -eq 0 ] || { echo "Cần chạy bằng root (sudo)"; exit 1; }
. /etc/os-release
[ "$VERSION_ID" = "24.04" ] || echo "!! Cảnh báo: máy đang chạy Ubuntu $VERSION_ID, đề yêu cầu 24.04"

echo ">> 1. Tạo người dùng quản trị $ADMIN_USER"
if ! id "$ADMIN_USER" >/dev/null 2>&1; then
  adduser --disabled-password --gecos "EduZone admin" "$ADMIN_USER"
fi
usermod -aG sudo "$ADMIN_USER"
echo "$ADMIN_USER ALL=(ALL) NOPASSWD:ALL" > "/etc/sudoers.d/90-$ADMIN_USER"
chmod 440 "/etc/sudoers.d/90-$ADMIN_USER"
visudo -cf "/etc/sudoers.d/90-$ADMIN_USER"

install -d -m 700 -o "$ADMIN_USER" -g "$ADMIN_USER" "/home/$ADMIN_USER/.ssh"
install -m 600 -o "$ADMIN_USER" -g "$ADMIN_USER" \
  "/home/$SOURCE_USER/.ssh/authorized_keys" "/home/$ADMIN_USER/.ssh/authorized_keys"

echo ">> 2. Khoá SSH: chỉ đăng nhập bằng khoá"
# Đặt tên 00- để đứng TRƯỚC 50-cloud-init.conf: sshd lấy giá trị xuất hiện đầu tiên.
cat > /etc/ssh/sshd_config.d/00-eduzone-hardening.conf <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
PubkeyAuthentication yes
EOF
sshd -t
systemctl reload ssh

echo ">> 3. Cài Docker Engine"
if ! command -v docker >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y ca-certificates curl
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
systemctl enable --now docker
usermod -aG docker "$ADMIN_USER"

echo ">> 4. Swap"
if ! swapon --show | grep -q .; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo
echo "== Xong. Kiểm tra:"
echo "   ssh -i <khoa.pem> $ADMIN_USER@<IP> 'docker version --format {{.Server.Version}}'"
sshd -T 2>/dev/null | grep -Ei '^(passwordauthentication|permitrootlogin|kbdinteractiveauthentication)'
