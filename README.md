# HƯỚNG DẪN SỬ DỤNG VÀ PHÂN TÍCH LUỒNG NGHIỆP VỤ HỆ THỐNG QLDG
**Phân hệ:** Import Lịch Giảng Dạy & Lập Kế Hoạch Dự Giờ (UC-07 → UC-13)  
**Địa chỉ mã nguồn:** `D:\Xuongptpm\qldg`  
**Công nghệ:** Backend Spring Boot 3 (Java 21) + Frontend Vue 3 (Vite) + Database MySQL / MariaDB  

---

## 1. TỔNG QUAN CÁC VAI TRÒ TRONG HỆ THỐNG (ROLES)

Hệ thống quản lý dữ liệu theo phạm vi (Scope) gồm **Cơ sở (Campus)** và **Bộ môn (Faculty)**. Có 6 vai trò chính:

| STT | Mã vai trò | Tên vai trò | Phạm vi dữ liệu | Trách nhiệm chính |
|---|---|---|---|---|
| 1 | **HEAD_OFFICE** | Ban Đào tạo Tổng (Super Admin) | Toàn quốc (14 cơ sở, tất cả bộ môn) | Cấu hình kỳ học, giám sát toàn hệ thống, có quyền xóa dữ liệu và duyệt cấp cao. |
| 2 | **TBDT** | Trưởng ban Đào tạo cơ sở | Cơ sở trực thuộc (tất cả bộ môn tại cơ sở) | Phê duyệt/từ chối/yêu cầu sửa kế hoạch dự giờ, xem lịch sử đối soát lịch dạy. |
| 3 | **TCQLDT** | Tổ chức & Quản lý Đào tạo | Cơ sở trực thuộc | Tiếp nhận file xếp lịch (Excel) từ phòng Đào tạo, thực hiện Import và đối soát lịch giảng dạy. |
| 4 | **CNBM** | Chủ nhiệm Bộ môn | Cơ sở trực thuộc + Bộ môn phụ trách | Lập kế hoạch dự giờ chính thức/đột xuất, phân công giảng viên dự giờ, chỉnh sửa kế hoạch khi TBĐT yêu cầu. |
| 5 | **TM** | Trưởng môn | Cơ sở trực thuộc + Bộ môn phụ trách | Đồng cấp với CNBM trong việc lập kế hoạch và phân công người dự giờ cho bộ môn. |
| 6 | **GV** | Giảng viên | Cá nhân | Được dự giờ hoặc được phân công đi dự giờ chéo (xem lịch cá nhân, không có quyền lập kế hoạch). |

---

## 2. PHÂN TÍCH LUỒNG SỬ DỤNG THEO TỪNG VAI TRÒ

### 2.1. VAI TRÒ: TỔ CHỨC & QUẢN LÝ ĐÀO TẠO (`TCQLDT`)
**Mục tiêu:** Đưa toàn bộ lịch dạy thật của cơ sở vào hệ thống làm căn cứ lập kế hoạch dự giờ.

* **Bước 1: Chuẩn bị file Excel lịch dạy**
  * Vào menu **Import lịch dạy** (`/import`).
  * Có thể bấm nút **Tải file mẫu** để lấy đúng cấu trúc cột chuẩn (Mã lớp, Mã môn, Tên môn, Giảng viên, Email, Ngày học, Ca 1-6, Phòng...).
* **Bước 2: Tải lên lịch dạy (UC-07)**
  * Chọn **Kỳ học** (VD: Fall 2026), **Block** (Block 1 hoặc Block 2).
  * Kéo thả hoặc chọn file `.xlsx` / `.xls`.
  * Bấm **Tải lên & Xử lý**. Hệ thống đẩy vào hàng đợi ngầm (`queue_tasks`) để xử lý bất đồng bộ, không treo trình duyệt.
* **Bước 3: Xem kết quả và đối soát dữ liệu (UC-08)**
  * Khi trạng thái chuyển sang **SUCCESS**, bấm nút **Xem đối soát** (`/import/:id`).
  * **Tab Tổng quan:** Xem thẻ tổng số lớp, số buổi, số giảng viên được ghi nhận.
  * **Tab Chi tiết đối soát:** Lọc theo 6 kết quả đối soát thực tế:
    * `INSERTED`: Buổi học mới hoàn toàn.
    * `SKIPPED`: Buổi học trùng khớp hoàn toàn với dữ liệu cũ (bỏ qua).
    * `UPDATED_ROOM`: Đổi phòng học.
    * `UPDATED_TIME`: Đổi ngày học hoặc ca học.
    * `SWAPPED`: Đổi giảng viên đứng lớp.
    * `ERROR`: Dòng thiếu dữ liệu bắt buộc (có nút **Xuất danh sách lỗi ra Excel** để gửi phòng đào tạo sửa).
  * **Tab Môn học mới:** Cảnh báo các mã môn xuất hiện lần đầu trong hệ thống.

---

### 2.2. VAI TRÒ: CHỦ NHIỆM BỘ MÔN (`CNBM`) & TRƯỞNG MÔN (`TM`)
**Mục tiêu:** Lập kế hoạch dự giờ chính thức đầu kỳ hoặc kế hoạch đột xuất khi có yêu cầu.

#### A. Luồng Lập Kế Hoạch Dự Giờ Chính Thức (UC-09, UC-10, UC-11, UC-13)
* **Bước 1: Chọn kỳ, block và bộ môn (UC-09)**
  * Vào menu **Lập KH dự giờ** (`/plans/create`).
  * Chọn **Kỳ**, **Block**, **Bộ môn** → Bấm **Xem danh sách lớp**.
  * Widget **Chỉ tiêu 15%** hiển thị số GV cần dự tối thiểu (15% số GV có lớp dạy trong block, làm tròn lên) và tiến độ hiện tại; ở bước phân công widget cập nhật ngay theo số GV được dự.
* **Bước 2: Phân loại và tích chọn lớp dự giờ**
  * Hệ thống chia lớp thành 3 tab rõ ràng:
    1. **Bắt buộc dự giờ:** Các lớp mà GV dạy môn lần đầu hoặc nghỉ dạy 6 kỳ liên tiếp. Mặc định hệ thống đã chọn tất cả lớp này.
    2. **Chọn thêm:** Danh sách các lớp còn lại của bộ môn trong block. Người dùng tích chọn thêm lớp cần dự.
    3. **Đã có kế hoạch:** Các lớp đã nằm trong kế hoạch khác, hiển thị ở chế độ chỉ đọc để tránh phân công trùng lặp.
  * Bấm **Tiếp tục: Phân công →** để chuyển sang Bước 2.
* **Bước 3: Phân công buổi dự và người dự (UC-10, UC-11)**
  * Tại từng lớp:
    * **Buổi dự (Bắt buộc):** Chỉ được chọn từ các buổi học thật trong lịch (ca 1 đến 6).
    * **GV 1 (Bắt buộc):** Kéo thả thẻ giảng viên bộ môn từ cột trái vào, hoặc bấm tìm kiếm.
    * **GV 2 (Bắt buộc):** Giảng viên cùng bộ môn hoặc dự chéo.
    * **GV 3 (Tùy chọn):** Bổ sung nếu cần tổ chức dự đoàn đông.
  * **Dự giờ chéo bộ môn / chéo cơ sở (UC-11):** Bấm nút **tìm** ở ô GV → gõ từ 2 ký tự (họ tên, email, account). Hệ thống tìm kiếm toàn quốc trên 14 cơ sở và ưu tiên giảng viên cơ sở hiện tại.
* **Bước 4: Lưu nháp hoặc Trình duyệt**
  * **Lưu nháp (DRAFT):** Lưu lại để sửa sau, chưa gửi lên TBĐT.
  * **Gửi trình duyệt (SUBMITTED):** Hệ thống kiểm tra đủ GV1, GV2 và buổi dạy thật cho tất cả lớp đã chọn. Kế hoạch chuyển sang trạng thái chờ duyệt.
* **Bước 5: Chỉnh sửa khi bị yêu cầu sửa (UC-13)**
  * Nếu TBĐT phản hồi **Yêu cầu sửa**, kế hoạch có trạng thái `REJECTED_NEED_EDIT`.
  * CNBM vào chi tiết kế hoạch xem lý do/ghi chú của TBĐT, bấm **Sửa kế hoạch**, điều chỉnh lại người dự hoặc lớp học, sau đó gửi duyệt lại.

#### B. Luồng Lập Kế Hoạch Dự Giờ Đột Xuất (UC-12)
* Áp dụng khi có phản ánh từ sinh viên hoặc ban thanh tra cần dự đột xuất.
* Vào menu **Quản lý KH dự giờ** → Bấm nút đỏ **+ KH dự giờ đột xuất** (`/plans/unscheduled`).
* Hệ thống hiển thị toàn bộ lớp đang dạy trong block của bộ môn (gom theo từng giảng viên).
* Người lập bắt buộc nhập **Lý do đột xuất** cho từng lớp được chọn.
* Mã kế hoạch tự động sinh hậu tố `-DX` và đi vào luồng phê duyệt riêng.
* Giảng viên được dự đột xuất vẫn tính vào chỉ tiêu 15%.

---

### 2.3. VAI TRÒ: TRƯỞNG BAN ĐÀO TẠO CƠ SỞ (`TBDT`)
**Mục tiêu:** Kiểm soát chất lượng, thẩm định và phê duyệt các kế hoạch dự giờ của toàn cơ sở.

* **Bước 1: Theo dõi danh sách kế hoạch (UC-13)**
  * Vào menu **Quản lý KH dự giờ** (`/plans`).
  * Lọc theo Bộ môn, Kỳ, Block hoặc Trạng thái (**Chờ duyệt**, **Đã duyệt**, **Yêu cầu sửa**).
* **Bước 2: Thẩm định chi tiết kế hoạch**
  * Bấm xem chi tiết kế hoạch (`/plans/:id`).
  * Kiểm tra danh sách lớp, ngày dự kiến, giảng viên được dự và các giảng viên đi dự giờ (GV1, GV2, GV3).
  * Xem dòng thời gian lịch sử phê duyệt (ai tạo, ai gửi, gửi lúc nào).
* **Bước 3: Thực hiện hành động phê duyệt**
  * Thanh hành động ghim cố định ở đáy màn hình với 3 lựa chọn:
    1. **Phê duyệt kế hoạch:** Kế hoạch chuyển sang `APPROVED`. Buổi dự giờ chính thức có hiệu lực trên toàn cơ sở.
    2. **Yêu cầu sửa:** Bắt buộc nhập nội dung ghi chú (VD: *Thay đổi GV2 ở lớp WD18301 do trùng lịch*). Kế hoạch chuyển sang `REJECTED_NEED_EDIT` và thông báo về cho CNBM.
    3. **Từ chối:** Bắt buộc nhập lý do từ chối. Kế hoạch chuyển sang `CANCELLED`.

---

### 2.4. VAI TRÒ: BAN ĐÀO TẠO TỔNG (`HEAD_OFFICE` - SUPER ADMIN)
**Mục tiêu:** Quản trị toàn quốc, giám sát dữ liệu và dọn dẹp các bản ghi nhầm lẫn.

* Xem toàn bộ dữ liệu của tất cả 14 cơ sở (Hà Nội, Đà Nẵng, TP.HCM, Cần Thơ, Tây Nguyên, Hải Phòng, Quy Nhơn, Thái Nguyên, Nam Định, Bắc Giang...).
* Chuyển đổi linh hoạt giữa các cơ sở trên thanh Dev Banner để kiểm tra chéo.
* Có quyền thực hiện phê duyệt thay cho TBĐT cơ sở khi cần xử lý khẩn cấp.
* **Quyền xóa an toàn:** Có quyền xóa cả các kế hoạch đã duyệt (`APPROVED`) kèm hộp thoại xác nhận cảnh báo mạnh để tránh xoá nhầm dữ liệu lịch sử.

---

### 2.5. VAI TRÒ: GIẢNG VIÊN (`GV`)
**Mục tiêu:** Nhận thông tin về các buổi dự giờ.

* Giảng viên không có quyền lập hoặc duyệt kế hoạch dự giờ.
* Sau khi kế hoạch được TBĐT phê duyệt (`APPROVED`), giảng viên sẽ nhận được thông báo về buổi được dự hoặc lịch đi dự giờ lớp của đồng nghiệp.

---

## 3. TÓM TẮT MA TRẬN PHÂN QUYỀN TRÊN MENU

| Chức năng / Đường dẫn | TCQLDT | CNBM / TM | TBDT | HEAD_OFFICE | GV |
|---|:---:|:---:|:---:|:---:|:---:|
| **Import lịch giảng dạy** (`/import`) | **Có** | Không | **Có** | **Có** | Không |
| **Chi tiết & Đối soát lịch dạy** (`/import/:id`) | **Có** | Không | **Có** | **Có** | Không |
| **Lập KH dự giờ chính thức** (`/plans/create`) | Không | **Có** | **Có** | **Có** | Không |
| **Lập KH dự giờ đột xuất** (`/plans/unscheduled`) | Không | **Có** | **Có** | **Có** | Không |
| **Quản lý KH dự giờ** (`/plans`) | Xem | **Toàn quyền** | **Duyệt / Sửa / Hủy** | **Toàn quyền** | Không |
| **Phê duyệt kế hoạch (Approve/Reject/Edit)** | Không | Không | **Có** | **Có** | Không |

---

## 4. HƯỚNG DẪN KIỂM THỬ NHANH VỚI DEV BANNER
Ở môi trường phát triển, hệ thống tích hợp thanh **Dev Banner** trên cùng màn hình cho phép chuyển đổi vai trò tức thì:
* Bấm chọn **Trưởng ban Đào tạo (TBDT)** để thử nghiệm tính năng Duyệt / Yêu cầu sửa / Từ chối.
* Bấm chọn **CNBM CNTT** hoặc **TM Lập trình Web** để thử nghiệm Lập kế hoạch, kéo thả phân công, tìm kiếm người dự chéo.
* Bấm chọn **TC&QL Đào tạo (TCQLDT)** để thử nghiệm Tải lên file Excel lịch dạy và xem đối soát.
* Bấm chọn **Super Admin (Head Office)** để thử nghiệm giám sát toàn quốc và xoá kế hoạch.
