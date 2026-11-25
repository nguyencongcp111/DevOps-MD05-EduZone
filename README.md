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
| `GHCR_OWNER` | — | _(chỉ máy chủ)_ Chủ sở hữu image trên ghcr.io, chữ thường |
| `IMAGE_TAG` | — | _(chỉ máy chủ)_ Tag image cần chạy = 7 ký tự đầu commit; `deploy.sh` tự điền |
| `GATEWAY_BIND` | `127.0.0.1` | _(chỉ máy chủ)_ Địa chỉ gateway lắng nghe; `127.0.0.1` = chỉ Nginx gọi được |
| `GRAFANA_ADMIN_PASSWORD` | `admin_local` | Mật khẩu tài khoản `admin` của Grafana (máy chủ: bắt buộc đổi) |
| `GRAFANA_ROOT_URL` | `http://localhost:3000/grafana/` | Địa chỉ công khai của Grafana (máy chủ: `https://nptc.io.vn/grafana/`) |

Trên máy chủ các biến nằm trong `~/eduzone/.env` (mẫu: `deploy/.env.prod.example`), mật khẩu **bắt buộc** phải đặt.

Biến bên trong từng service (Compose tự gán): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, và với enrollment-service/gateway thêm `COURSE_SERVICE_URL`, `STUDENT_SERVICE_URL`, `ENROLLMENT_SERVICE_URL`, `ACCESS_SERVICE_URL`.

## CI/CD — GitHub Actions

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

| Job | Khi nào chạy | Việc làm |
|---|---|---|
| `test` (5 job song song) | Mọi push và pull request vào `main` | `mvn verify`: build và chạy unit test từng service |
| `image` (5 job song song) | Chỉ khi push lên `main` và `test` đã xanh | Build image, đẩy lên `ghcr.io/<owner>/eduzone-<service>` với tag **7 ký tự đầu của commit** và `latest` |

## Triển khai lên máy chủ

Máy chủ: Ubuntu 24.04. Hệ thống chạy bằng image **kéo từ ghcr.io**, không build trên máy chủ.

**1. Khởi tạo máy chủ (một lần):** tạo người dùng quản trị `eduadmin`, chỉ cho đăng nhập bằng khoá, cài Docker.

```bash
ssh -i <khoa.pem> ubuntu@<IP> 'sudo bash -s -- eduadmin' < scripts/server-bootstrap.sh
```

**2. Triển khai một commit đã qua CI:**

```bash
cp deploy/.env.prod.example deploy/.env.prod   # rồi đổi mật khẩu
SSH_KEY=<khoa.pem> bash scripts/deploy.sh eduadmin@<IP>
```

Script chép `docker-compose.prod.yml`, script tạo database và `.env` lên `~/eduzone`, ghi `IMAGE_TAG` = commit hiện tại, rồi `docker compose pull && up -d`.

**Quay lui phiên bản:** `bash scripts/deploy.sh eduadmin@<IP> <tag-cũ>`.

`deploy.sh` cũng chép và dựng luôn cụm giám sát (`observability/monitoring`) vào `~/eduzone/monitoring`.

**3. Nginx + HTTPS (một lần, sau khi tên miền đã trỏ về máy chủ):**

```bash
SSH_KEY=<khoa.pem> bash scripts/setup-https.sh eduadmin@<IP> nptc.io.vn <email>
```

Script cài Nginx và certbot trên máy chủ, xin chứng chỉ Let's Encrypt (xác minh qua `/.well-known/acme-challenge/`), cài [`deploy/nginx/eduzone.conf`](deploy/nginx/eduzone.conf) và bật tự gia hạn (`certbot.timer` + nạp lại Nginx sau khi gia hạn).

## Che chắn mạng

| Thành phần | Lắng nghe | Từ Internet |
|---|---|---|
| Nginx | `0.0.0.0:80`, `0.0.0.0:443` | ✅ Cổng duy nhất mở |
| gateway | `127.0.0.1:8080` (`GATEWAY_BIND=127.0.0.1`) | ❌ chỉ qua Nginx `/api/` |
| Grafana | `127.0.0.1:3000` | ❌ chỉ qua Nginx `/grafana/` |
| Prometheus | `127.0.0.1:9090` | ❌ xem qua đường hầm SSH |
| 4 service, Postgres, node-exporter | chỉ mạng Docker `eduzone_backend` | ❌ |

Docker ghi luật iptables riêng nên **ufw không chặn được cổng Docker đã publish** — vì vậy các cổng được gắn vào `127.0.0.1` ngay trong Compose, cộng với Security Group của EC2 chỉ mở 22, 80, 443.

Kiểm tra từ máy khác (chỉ thấy 22, 80, 443):

```bash
nmap -Pn -p 22,80,443,3000,5432,8080,8081-8084,9090,9100 nptc.io.vn
```

## Giám sát — Prometheus + Grafana

- **Prometheus** thu `/actuator/prometheus` của 4 service + gateway (job `eduzone-services`, `gateway`) và `node-exporter` (tài nguyên máy chủ). Luật cảnh báo trong [`alerts.yml`](observability/monitoring/prometheus/alerts.yml): service chết, tỉ lệ lỗi 5xx > 5%, tỉ lệ ghi danh bị huỷ > 50%.
- **Grafana** tự nạp datasource và dashboard **EduZone — Vận hành** khi khởi động; xem **không cần đăng nhập** (Viewer ẩn danh), sửa cần tài khoản admin.
- Số đo nghiệp vụ riêng: `eduzone_enrollments_total{status, reason}` (counter) do enrollment-service ghi — phát hiện được ghi danh bị huỷ dù API vẫn trả `HTTP 200`.

| Biểu đồ | Loại số đo | Câu hỏi vận hành |
|---|---|---|
| Lưu lượng request theo service | counter (`rate`) | Service nào nhận nhiều tải nhất? Có tăng đột biến không? |
| Ghi danh mỗi phút theo kết quả | counter nghiệp vụ | Bao nhiêu ghi danh thành công / bị huỷ, huỷ vì lý do gì? |
| Tỉ lệ lỗi HTTP theo service | counter (tỉ lệ) | Service nào đang trả lỗi cho client? |
| Độ trễ p95 theo service | histogram | 95% request được trả lời trong bao lâu? Service nào chậm? |
| Bộ nhớ heap JVM | gauge | Service nào sắp chạm giới hạn 512 MB? |
| Kết nối Postgres đang dùng | gauge | Pool kết nối database có bị cạn không? |
| CPU máy chủ | gauge (node-exporter) | Máy chủ có quá tải CPU không? |
| RAM và đĩa máy chủ | gauge (node-exporter) | Còn đủ RAM/đĩa cho ứng dụng, giám sát và log không? |

Chạy trên máy cá nhân (sau khi ứng dụng đã lên):

```bash
docker compose -f observability/monitoring/docker-compose.yml up -d
```

Grafana: <http://localhost:3000/grafana/> · Prometheus: <http://localhost:9090/targets>

Trên máy chủ, xem trang Targets qua đường hầm SSH rồi mở <http://localhost:9090/targets>:

```bash
ssh -i <khoa.pem> -L 9090:127.0.0.1:9090 eduadmin@<IP>
```

## Địa chỉ công khai

- API: <https://nptc.io.vn/api/courses>
- Dashboard: <https://nptc.io.vn/grafana/>

## Phân công công việc

| Thành viên | Hạng mục phụ trách |
|---|---|
| _Họ tên 1_ | |
| _Họ tên 2_ | |
| _Họ tên 3_ | |
