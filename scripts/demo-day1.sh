#!/usr/bin/env bash
# Kiểm tra mốc ngày 1: luồng thành công, luồng bị từ chối, và cô lập database.
# Chạy từ thư mục gốc repo:  bash scripts/demo-day1.sh
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
COMPOSE="docker compose -f deploy/docker-compose.yml"
JSON='Content-Type: application/json'

step() { printf '\n\033[1;36m== %s\033[0m\n' "$*"; }

step "Danh sách khoá học"
curl -s "$BASE/api/courses"; echo

step "Ví học viên 1 TRƯỚC khi ghi danh"
curl -s "$BASE/api/students/1"; echo

step "1) Ghi danh THÀNH CÔNG: học viên 1 vào DEVOPS-101 (kỳ vọng CONFIRMED)"
curl -s -X POST "$BASE/api/enrollments" -H "$JSON" -d '{"studentId":1,"courseCode":"DEVOPS-101"}'; echo

step "Bật điều kiện từ chối: đánh dấu lớp SPRING-201 đã đủ sĩ số"
curl -s -X PUT "$BASE/api/access/class-locks/SPRING-201"; echo

step "Ví học viên 1 trước đơn bị huỷ"
curl -s "$BASE/api/students/1"; echo

step "2) Ghi danh BỊ TỪ CHỐI: học viên 1 vào SPRING-201 (kỳ vọng HTTP 200 + CANCELLED)"
curl -s -w '\nHTTP %{http_code}\n' -X POST "$BASE/api/enrollments" -H "$JSON" -d '{"studentId":1,"courseCode":"SPRING-201"}'

step "Ví học viên 1 SAU đơn bị huỷ (kỳ vọng không đổi so với trước)"
curl -s "$BASE/api/students/1"; echo

step "Suất SPRING-201 (kỳ vọng vẫn 20, đã trả lại)"
curl -s "$BASE/api/courses" | grep -o '"code":"SPRING-201"[^}]*'

step "Tắt điều kiện từ chối"
curl -s -X DELETE "$BASE/api/access/class-locks/SPRING-201"; echo

step "Cô lập database: course_user cố kết nối eduzone_student_db (kỳ vọng permission denied)"
$COMPOSE exec -T -e PGPASSWORD="${COURSE_DB_PASSWORD:-course_pass}" postgres \
  psql -h 127.0.0.1 -U course_user -d eduzone_student_db -c 'select 1' 2>&1 || true

step "Endpoint nội bộ KHÔNG đi qua gateway (kỳ vọng 404)"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X POST "$BASE/internal/courses/DEVOPS-101/reserve"
