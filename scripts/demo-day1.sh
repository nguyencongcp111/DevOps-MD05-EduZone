#!/usr/bin/env bash
# Kiểm tra luồng nghiệp vụ: ghi danh thành công, ghi danh bị từ chối (lớp đủ sĩ số), cô lập database.
# Mỗi lần chạy tạo một học viên MỚI có 10.000.000 VND, nên chạy lại bao nhiêu lần cũng được.
#
#   bash scripts/demo-day1.sh                                  # máy cá nhân (http://localhost:8080)
#   BASE_URL=https://nptc.io.vn bash scripts/demo-day1.sh      # máy chủ thật
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
COMPOSE="docker compose -f deploy/docker-compose.yml"
JSON='Content-Type: application/json'
LOCKED_COURSE="SPRING-201"

step() { printf '\n\033[1;36m== %s\033[0m\n' "$*"; }
json_id() { { grep -o '"id":[0-9]*' || true; } | head -1 | cut -d: -f2; }
seats() { curl -s "$BASE/api/courses" | grep -o "\"code\":\"$1\"[^}]*" | grep -o '"seatsAvailable":[0-9]*' | cut -d: -f2; }

echo "Địa chỉ: $BASE"

step "Tạo học viên mới cho lần chạy này (ví 10.000.000 VND)"
STAMP="$(date +%s)"
STUDENT_JSON="$(curl -s -X POST "$BASE/api/students" -H "$JSON" \
  -d "{\"fullName\":\"Hoc vien demo $STAMP\",\"email\":\"demo$STAMP@eduzone.local\",\"walletBalance\":10000000}")"
echo "$STUDENT_JSON"
SID="$(echo "$STUDENT_JSON" | json_id)"
[ -n "$SID" ] || { echo "Không tạo được học viên — hệ thống có đang chạy tại $BASE không?"; exit 1; }

step "1) Ghi danh THÀNH CÔNG: học viên $SID vào DEVOPS-101 (kỳ vọng CONFIRMED)"
curl -s -w '\nHTTP %{http_code}\n' -X POST "$BASE/api/enrollments" -H "$JSON" \
  -d "{\"studentId\":$SID,\"courseCode\":\"DEVOPS-101\"}"

step "Ví học viên $SID sau ghi danh thành công (kỳ vọng 8.500.000)"
curl -s "$BASE/api/students/$SID"; echo

step "Bật điều kiện từ chối: đánh dấu lớp $LOCKED_COURSE đã đủ sĩ số"
curl -s -X PUT "$BASE/api/access/class-locks/$LOCKED_COURSE"; echo
SEATS_BEFORE="$(seats "$LOCKED_COURSE")"

step "2) Ghi danh BỊ TỪ CHỐI: học viên $SID vào $LOCKED_COURSE (kỳ vọng HTTP 200 + CANCELLED, lớp đủ sĩ số)"
curl -s -w '\nHTTP %{http_code}\n' -X POST "$BASE/api/enrollments" -H "$JSON" \
  -d "{\"studentId\":$SID,\"courseCode\":\"$LOCKED_COURSE\"}"

step "Ví học viên $SID sau đơn bị huỷ (kỳ vọng vẫn 8.500.000 — đã hoàn tiền)"
curl -s "$BASE/api/students/$SID"; echo

step "Suất $LOCKED_COURSE (kỳ vọng không đổi — đã trả lại suất)"
echo "trước: $SEATS_BEFORE   sau: $(seats "$LOCKED_COURSE")"

step "Tắt điều kiện từ chối"
curl -s -X DELETE "$BASE/api/access/class-locks/$LOCKED_COURSE"; echo

step "Endpoint nội bộ KHÔNG đi qua gateway (kỳ vọng 404)"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X POST "$BASE/internal/courses/DEVOPS-101/reserve"

step "Cô lập database: course_user cố kết nối eduzone_student_db (kỳ vọng permission denied)"
if [ "$BASE" != "http://localhost:8080" ]; then
  echo "(bỏ qua — chỉ kiểm được khi chạy trên máy có container postgres)"
elif ! $COMPOSE ps --status running --services 2>/dev/null | grep -qx postgres; then
  echo "(bỏ qua — Docker mà shell này đang dùng không thấy container postgres của EduZone."
  echo " Trong WSL: bật Docker Desktop > Settings > Resources > WSL integration cho distro này,"
  echo " hoặc chạy script từ Git Bash.)"
else
  $COMPOSE exec -T -e PGPASSWORD="${COURSE_DB_PASSWORD:-course_pass}" postgres \
    psql -h 127.0.0.1 -U course_user -d eduzone_student_db -c 'select 1' 2>&1 || true
fi
