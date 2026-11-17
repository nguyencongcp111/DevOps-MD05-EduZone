# EduZone — Hệ thống ghi danh khoá học trực tuyến

Đồ án cuối môn DevOps, **biến thể 5 — Khoá học trực tuyến**.
Bốn microservice Spring Boot và một cổng vào, mỗi service một database PostgreSQL riêng.

| Thành phần | Trách nhiệm | Cổng nội bộ | Database |
|---|---|---|---|
| `gateway` | Cổng vào duy nhất (Spring Cloud Gateway), định tuyến `/api/**` | 8080 | — |
| `course-service` | Khoá học, học phí, số suất ghi danh còn mở | 8081 | `eduzone_course_db` |
| `student-service` | Học viên và số dư ví | 8082 | `eduzone_student_db` |
| `enrollment-service` | Tạo ghi danh và điều phối toàn bộ luồng | 8083 | `eduzone_enrollment_db` |
| `access-service` | Cấp quyền vào lớp — **có quyền từ chối** khi lớp đủ sĩ số | 8084 | `eduzone_access_db` |

Bên ngoài chỉ vào được qua `gateway`. Các service gọi thẳng nhau trong mạng Docker nội bộ qua đường dẫn `/internal/**`, gateway không định tuyến đường dẫn này.

## Luồng ghi danh 6 bước

| Bước | Service | Việc làm |
|---|---|---|
| 1 | enrollment | Tạo ghi danh `PENDING` |
| 2 | course | Kiểm tra và giữ 1 suất |
| 3 | student | Trừ học phí trong ví |
| 4 | access | Cấp quyền vào lớp — **hoặc từ chối** (`409`, lớp đã đủ sĩ số) |
| 5 | student, course | Nếu bị từ chối: hoàn học phí, trả lại suất |
| 6 | enrollment | Kết thúc `CONFIRMED` hoặc `CANCELLED` (API luôn trả `HTTP 200`) |

**Kịch bản sự cố có kiểm soát:** `PUT /api/access/class-locks/{courseCode}` đánh dấu lớp đã đủ sĩ số, `DELETE` để mở lại.
- `access-service` ghi `WARN` **không kèm ngoại lệ** → nguyên nhân gốc.
- `enrollment-service` ghi `WARN` **kèm** `HttpClientErrorException$Conflict` → hậu quả.

## Dựng trên máy cá nhân (một lệnh)

Yêu cầu: Docker Desktop (cấp tối thiểu 6 GB RAM). Không cần cài Java hay Maven — image được build bằng Maven bên trong Docker.

```bash
docker compose -f deploy/docker-compose.yml up -d --build --wait
```

Kiểm tra toàn bộ mốc ngày 1 (luồng thành công, luồng bị huỷ, cô lập database):

```bash
bash scripts/demo-day1.sh
```

Dừng hệ thống (thêm `-v` để xoá luôn dữ liệu):

```bash
docker compose -f deploy/docker-compose.yml down
```

## API qua gateway

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| `POST` | `/api/enrollments` | Ghi danh — body `{"studentId":1,"courseCode":"DEVOPS-101"}` |
| `GET` | `/api/enrollments/{id}` | Xem ghi danh |
| `GET` / `POST` | `/api/courses` | Danh sách / tạo khoá học |
| `POST` | `/api/students` | Tạo học viên |
| `GET` | `/api/students/{id}` | Xem học viên và số dư ví |
| `PUT` / `DELETE` | `/api/access/class-locks/{courseCode}` | Bật / tắt điều kiện từ chối |
| `GET` | `/api/access/grants?studentId=` | Tra cứu quyền vào lớp |

Dữ liệu mẫu tự tạo khi khởi động: khoá `DEVOPS-101`, `SPRING-201`, `K8S-301`; học viên `1` (10.000.000 VND) và `2` (3.000.000 VND).

## Biến môi trường

Đặt trong `deploy/.env` (mẫu: `deploy/.env.example`). Mọi biến đều có giá trị mặc định cho máy cá nhân.

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `POSTGRES_PASSWORD` | `postgres_admin` | Mật khẩu superuser PostgreSQL |
| `COURSE_DB_PASSWORD` | `course_pass` | Mật khẩu `course_user` |
| `STUDENT_DB_PASSWORD` | `student_pass` | Mật khẩu `student_user` |
| `ENROLLMENT_DB_PASSWORD` | `enrollment_pass` | Mật khẩu `enrollment_user` |
| `ACCESS_DB_PASSWORD` | `access_pass` | Mật khẩu `access_user` |
| `GATEWAY_PORT` | `8080` | Cổng gateway mở ra máy host |

Biến bên trong từng service (Compose tự gán): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, và với enrollment-service/gateway thêm `COURSE_SERVICE_URL`, `STUDENT_SERVICE_URL`, `ENROLLMENT_SERVICE_URL`, `ACCESS_SERVICE_URL`.

## Địa chỉ công khai

_Cập nhật ở ngày 3: `https://<tên-miền>`_

## Phân công công việc

| Thành viên | Hạng mục phụ trách |
|---|---|
| _Họ tên 1_ | |
| _Họ tên 2_ | |
| _Họ tên 3_ | |
