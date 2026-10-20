# TÀI LIỆU KỊCH BẢN KIỂM THỬ HỆ THỐNG QLDG (TEST SCENARIOS & TEST CASES)
**Dự án:** Quản lý Dự giờ Giảng dạy (QLDG)  
**Phân hệ kiểm thử:** Import Lịch Giảng Dạy & Lập Kế Hoạch Dự Giờ (UC-07 → UC-13)  
**Vị trí mã nguồn:** `D:\Xuongptpm\qldg`  
**Môi trường thử nghiệm:**
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`
- Database: MySQL / MariaDB (Port 3306, DB `qldg`)
- Công cụ hỗ trợ: Thanh **Dev Banner** trên đầu trang để chuyển đổi vai trò (Role) tức thì không cần đăng nhập lại.

---

## MỤC LỤC
1. [Danh sách tài khoản kiểm thử theo vai trò](#1-danh-sách-tài-khoản-kiểm-thử-theo-vai-trò)
2. [Kịch bản 1: Phân quyền truy cập & Menu theo Vai trò](#kịch-bản-1-phân-quyền-truy-cập--menu-theo-vai-trò)
3. [Kịch bản 2: Import Lịch Giảng Dạy (UC-07 - TCQLDT)](#kịch-bản-2-import-lịch-giảng-dạy-uc-07---tcqldt)
4. [Kịch bản 3: Đối Soát Lịch Giảng Dạy (UC-08 - TCQLDT, TBDT)](#kịch-bản-3-đối-soát-lịch-giảng-dạy-uc-08---tcqldt-tbdt)
5. [Kịch bản 4: Tự Động Phân Loại Lớp Học Phần & Chỉ Tiêu 15% (UC-09 - CNBM, TM)](#kịch-bản-4-tự-động-phân-loại-lớp-học-phần--chỉ-tiêu-15-uc-09---cnbm-tm)
6. [Kịch bản 5: Lập Kế Hoạch Dự Giờ Chính Thức (UC-10 - CNBM, TM)](#kịch-bản-5-lập-kế-hoạch-dự-giờ-chính-thức-uc-10---cnbm-tm)
7. [Kịch bản 6: Phân Công Dự Giờ Chéo Bộ Môn / Cơ Sở (UC-11 - CNBM, TM)](#kịch-bản-6-phân-công-dự-giờ-chéo-bộ-môn--cơ-sở-uc-11---cnbm-tm)
8. [Kịch bản 7: Lập Kế Hoạch Dự Giờ Đột Xuất (UC-12 - CNBM, TM)](#kịch-bản-7-lập-kế-hoạch-dự-giờ-đột-xuất-uc-12---cnbm-tm)
9. [Kịch bản 8: Luồng Phê Duyệt & Vòng Đời Kế Hoạch (UC-13 - TBDT, CNBM, HEAD_OFFICE)](#kịch-bản-8-luồng-phê-duyệt--vòng-đời-kế-hoạch-uc-13---tbdt-cnbm-head_office)

---

## 1. DANH SÁCH TÀI KHOẢN KIỂM THỬ THEO VAI TRÒ

| STT | Tài khoản (Account) | Họ và tên | Vai trò (Role) | Cơ sở (Campus) | Bộ môn (Faculty) | Mục đích kiểm thử |
|:---:|---|---|:---:|:---:|:---:|---|
| 1 | `ho_admin` | Nguyễn Văn Hùng | `HEAD_OFFICE` | Toàn quốc | Tất cả | Super Admin, xóa an toàn kế hoạch đã duyệt, xem 14 cơ sở |
| 2 | `loanth` | Hoàng Thị Loan | `TBDT` | Đà Nẵng (dna) | Cơ sở | Trưởng ban Đào tạo: Phê duyệt, Yêu cầu sửa, Từ chối |
| 3 | `tuanna` | Nguyễn Anh Tuấn | `TCQLDT` | Đà Nẵng (dna) | Cơ sở | Tải lên Excel lịch dạy, kiểm tra đối soát 6 trạng thái |
| 4 | `dungpt` | Phan Tiến Dũng | `CNBM` | Đà Nẵng (dna) | CNTT | Chủ nhiệm bộ môn CNTT: Lập KH, chọn lớp, phân công GV |
| 5 | `haidv` | Đỗ Văn Hải | `TM` | Đà Nẵng (dna) | CNTT | Trưởng môn CNTT: Lập KH chính thức / đột xuất |
| 6 | `nhungpt4` | Phạm Thị Nhung | `CNBM` | Đà Nẵng (dna) | Cơ bản (CB) | CNBM Cơ bản: Lập KH cho nhóm bộ môn liên môn (CB, TA, TT) |
| 7 | `namnv` | Nguyễn Văn Nam | `GV` | Đà Nẵng (dna) | CNTT | Giảng viên: Nhận dự giờ, được phân công dự chéo |
| 8 | `nhungpt_hn` | Phạm Thu Nhung | `GV` | Hà Nội (hn) | Cơ bản (CB) | Giảng viên khác cơ sở (dùng test dự giờ chéo toàn quốc) |
| 9 | `minhtv` | Trần Văn Minh | `GV` | TP.HCM (hcm) | CNTT | Giảng viên khác cơ sở (dùng test dự giờ chéo toàn quốc) |

---

## KỊCH BẢN 1: PHÂN QUYỀN TRUY CẬP & MENU THEO VAI TRÒ

### Test Case 1.1: Kiểm tra Menu hiển thị cho TC&QL Đào tạo (`TCQLDT`)
- **Tài khoản:** Chọn `tuanna (TCQLDT)` trên Dev Banner.
- **Các bước thực hiện:**
  1. Quan sát thanh Sidebar bên trái.
- **Kết quả mong đợi:**
  - Hiển thị menu **Import lịch dạy** và **Quản lý KH dự giờ**.
  - Không hiển thị menu **Lập KH dự giờ** (vì TCQLDT không lập kế hoạch).

### Test Case 1.2: Kiểm tra Menu hiển thị cho Chủ nhiệm Bộ môn (`CNBM`) và Trưởng môn (`TM`)
- **Tài khoản:** Chọn `dungpt (CNBM)` hoặc `haidv (TM)` trên Dev Banner.
- **Các bước thực hiện:**
  1. Quan sát thanh Sidebar bên trái.
- **Kết quả mong đợi:**
  - Hiển thị menu **Lập KH dự giờ** và **Quản lý KH dự giờ**.
  - Không hiển thị menu **Import lịch dạy** (chỉ TCQLDT/TBDT mới import).

### Test Case 1.3: Kiểm tra Menu hiển thị cho Trưởng ban Đào tạo (`TBDT`) và `HEAD_OFFICE`
- **Tài khoản:** Chọn `loanth (TBDT)` hoặc `ho_admin (HEAD_OFFICE)`.
- **Kết quả mong đợi:**
  - Hiển thị đầy đủ cả 3 menu: **Import lịch dạy**, **Lập KH dự giờ** (dự phòng khi cơ sở thiếu CNBM), và **Quản lý KH dự giờ**.

### Test Case 1.4: Kiểm tra thông báo khi tài khoản Giảng viên (`GV`) đăng nhập
- **Tài khoản:** Chọn `namnv (GV)`.
- **Kết quả mong đợi:**
  - Sidebar hiển thị thông báo: *"Tài khoản GV không có menu trong phân hệ Lập KH & Import"*.

---

## KỊCH BẢN 2: IMPORT LỊCH GIẢNG DẠY (UC-07 - TCQLDT)

### Test Case 2.1: Tải file mẫu Excel lịch giảng dạy
- **Tài khoản:** `tuanna (TCQLDT)`
- **Các bước thực hiện:**
  1. Vào trang **Import lịch dạy** (`/import`).
  2. Bấm nút **Tải file mẫu (.xlsx)** ở góc phải thẻ Tải lên.
- **Kết quả mong đợi:**
  - Trình duyệt tải về file `mau_import_lich_giang_day.xlsx`.
  - Mở file thấy đủ các cột chuẩn: Mã lớp, Mã môn, Tên môn, Giảng viên, Email, Ngày học, Ca (1-6), Phòng học.

### Test Case 2.2: Tải lên file sai định dạng
- **Tài khoản:** `tuanna (TCQLDT)`
- **Các bước thực hiện:**
  1. Chọn 1 file không phải Excel (VD: file `.pdf`, `.png` hoặc `.txt`).
  2. Kéo thả vào vùng tải lên.
- **Kết quả mong đợi:**
  - Hệ thống thông báo lỗi: *"Vui lòng chọn file định dạng Excel (.xlsx hoặc .xls)"*.
  - Nút Tải lên không được kích hoạt.

### Test Case 2.3: Tải lên file Excel lịch dạy hợp lệ
- **Tài khoản:** `tuanna (TCQLDT)`
- **Các bước thực hiện:**
  1. Chọn **Kỳ học** (Fall 2026), **Block** (Block 1).
  2. Chọn file Excel lịch dạy chuẩn.
  3. Bấm nút **Tải lên & Xử lý nền**.
- **Kết quả mong đợi:**
  - Bảng Lịch sử import xuất hiện dòng tác vụ mới với trạng thái ban đầu là `QUEUED` / `PROCESSING`.
  - Tiến trình (Progress bar) tăng dần đến 100%.
  - Trạng thái chuyển sang `SUCCESS` kèm số dòng thành công, số lỗi (nếu có).
  - Có nút **Xem đối soát** xuất hiện ở cột Thao tác.

---

## KỊCH BẢN 3: ĐỐI SOÁT LỊCH GIẢNG DẠY (UC-08 - TCQLDT, TBDT)

### Test Case 3.1: Xem tổng quan số liệu đối soát
- **Tài khoản:** `tuanna (TCQLDT)` hoặc `loanth (TBDT)`
- **Các bước thực hiện:**
  1. Tại bảng Lịch sử import, bấm nút **Xem đối soát** của đợt vừa tải lên.
  2. Xem nội dung tại **Tab 1: Tổng quan**.
- **Kết quả mong đợi:**
  - Hiển thị đầy đủ các thẻ số liệu thống kê: Tổng số dòng, Số lớp học phần, Số buổi học, Số môn học, Số giảng viên.

### Test Case 3.2: Kiểm tra phân loại 6 kết quả đối soát thực tế
- **Các bước thực hiện:**
  1. Chuyển sang **Tab 2: Chi tiết đối soát**.
  2. Thử lọc dropdown theo từng loại kết quả:
     - `INSERTED`: Buổi học mới thêm vào hệ thống.
     - `SKIPPED`: Buổi học trùng khớp 100% với dữ liệu đang có (bỏ qua không ghi đè).
     - `UPDATED_ROOM`: Buổi học có sự thay đổi phòng học.
     - `UPDATED_TIME`: Buổi học có sự đổi ngày hoặc ca học.
     - `SWAPPED`: Đổi giảng viên đứng lớp.
     - `ERROR`: Dòng thiếu thông tin bắt buộc.
- **Kết quả mong đợi:**
  - Bảng dữ liệu lọc chính xác theo từng trạng thái.
  - Mỗi dòng ghi rõ: Mã lớp, Môn học, Giảng viên cũ/mới, Ngày/Ca cũ/mới, Phòng cũ/mới.

### Test Case 3.3: Xuất danh sách dòng lỗi ra Excel
- **Các bước thực hiện:**
  1. Nếu có kết quả `ERROR`, quan sát nút đỏ **Xuất danh sách lỗi ra Excel** ở đầu tab.
  2. Bấm nút tải về.
- **Kết quả mong đợi:**
  - Trình duyệt tải về file Excel danh sách các dòng bị lỗi kèm cột ghi chú nguyên nhân lỗi để gửi phòng Đào tạo chỉnh sửa.

### Test Case 3.4: Cảnh báo môn học mới
- **Các bước thực hiện:**
  1. Chuyển sang **Tab 3: Môn học mới**.
- **Kết quả mong đợi:**
  - Hiển thị danh sách các mã môn học lần đầu xuất hiện trong hệ thống cần đặt tên hoặc gán bộ môn.

---

## KỊCH BẢN 4: TỰ ĐỘNG PHÂN LOẠI LỚP HỌC PHẦN & CHỈ TIÊU 15% (UC-09 - CNBM, TM)

### Test Case 4.1: Tính toán chỉ tiêu dự giờ 15% theo mẫu số GV có lớp dạy thật
- **Tài khoản:** `dungpt (CNBM CNTT)`
- **Các bước thực hiện:**
  1. Vào menu **Lập KH dự giờ** (`/plans/create`).
  2. Chọn Kỳ: **Fall 2026**, Block: **Block 1**, Bộ môn: **Công nghệ thông tin**.
  3. Bấm **Duyệt danh sách lớp**.
- **Kết quả mong đợi:**
  - Widget Chỉ tiêu 15% hiển thị rõ:
    - *Mẫu số:* Tổng số GV có lớp dạy thật trong block của bộ môn (không tính GV không có lịch).
    - *Chỉ tiêu tối thiểu:* `CEIL(Tổng GV × 15%)`.
    - Thanh tiến độ hiển thị phần trăm hoàn thành.

### Test Case 4.2: Phân loại lớp vào Tab "1. BẮT BUỘC DỰ GIỜ"
- **Các bước thực hiện:**
  1. Xem danh sách lớp trong Tab **Bắt buộc dự giờ**.
- **Kết quả mong đợi:**
  - Các lớp có giảng viên thuộc diện bắt buộc:
    - *Lần đầu dạy môn* (kể cả phiên bản môn mới như COM1081).
    - *Nghỉ dạy môn 6 kỳ liên tiếp*.
  - Có badge màu đỏ ghi rõ lý do bắt buộc.
  - **Mặc định hệ thống đã tự động tích chọn tất cả các lớp này**.

### Test Case 4.3: Chọn thêm lớp từ Tab "2. CHỌN THÊM"
- **Các bước thực hiện:**
  1. Bấm sang Tab **Chọn thêm**.
  2. Tích chọn thêm 1 hoặc nhiều lớp thông thường.
- **Kết quả mong đợi:**
  - Hàng được chọn đổi sang màu nền nổi bật.
  - Tổng số lớp đã chọn ở thanh chân trang tăng lên tương ứng.

### Test Case 4.4: Lớp đã thuộc kế hoạch khác tại Tab "3. ĐÃ CÓ KẾ HOẠCH"
- **Các bước thực hiện:**
  1. Bấm sang Tab **Đã có kế hoạch**.
- **Kết quả mong đợi:**
  - Hiển thị danh sách các lớp đã được đưa vào kế hoạch dự giờ trước đó.
  - Dòng hiển thị mờ, không có ô checkbox (chỉ đọc) để ngăn chặn việc phân công trùng lặp.

---

## KỊCH BẢN 5: LẬP KẾ HOẠCH DỰ GIỜ CHÍNH THỨC (UC-10 - CNBM, TM)

### Test Case 5.1: Chuyển sang Bước 2 (Phân công người dự)
- **Tài khoản:** `dungpt (CNBM CNTT)`
- **Các bước thực hiện:**
  1. Sau khi chọn các lớp ở Bước 1, bấm **Tiếp tục: Phân công →**.
- **Kết quả mong đợi:**
  - Chuyển sang màn hình phân công gồm:
    - Cột trái: Danh sách giảng viên bộ môn có thể kéo thả.
    - Cột phải: Các thẻ lớp học phần cần phân công.

### Test Case 5.2: Chọn ngày dự kiến từ buổi học thật
- **Các bước thực hiện:**
  1. Tại ô dropdown **Buổi dự** của từng lớp, bấm mở danh sách.
- **Kết quả mong đợi:**
  - Dropdown **chỉ hiển thị các buổi học THẬT** theo lịch import (ngày, ca 1-6, số thứ tự buổi, phòng học).
  - Không cho phép nhập ngày/ca tự do ngoài lịch dạy.

### Test Case 5.3: Kéo thả giảng viên từ cột trái vào ô GV1 / GV2
- **Các bước thực hiện:**
  1. Nhấp giữ chuột vào thẻ giảng viên ở cột trái (VD: thẻ `namnv`).
  2. Kéo và thả vào ô **GV 1** của lớp.
- **Kết quả mong đợi:**
  - Ô GV1 nhận đúng giảng viên, hiển thị chip tên tài khoản kèm nút xóa `✕`.
  - Thẻ lớp cập nhật tiến độ (viền đổi màu khi hoàn thành).

### Test Case 5.4: Kiểm tra tính năng Lưu nháp (`DRAFT`)
- **Các bước thực hiện:**
  1. Điền một phần thông tin (chưa đủ GV1, GV2 cho tất cả các lớp).
  2. Bấm nút **Lưu nháp**.
- **Kết quả mong đợi:**
  - Hệ thống lưu thành công bản nháp, chuyển về danh sách kế hoạch.
  - Kế hoạch có trạng thái badge xám **Bản nháp (DRAFT)**.

### Test Case 5.5: Kiểm tra ràng buộc khi Gửi trình duyệt (`SUBMITTED`)
- **Các bước thực hiện:**
  1. Bỏ trống ô GV2 hoặc ô Buổi dự của 1 lớp.
  2. Bấm nút **Gửi trình duyệt →**.
- **Kết quả mong đợi:**
  - Hệ thống chặn lại và thông báo: *"Lớp [Mã lớp] chưa đủ buổi dự, GV1 và GV2!"*.
  - Ô bị thiếu chuyển viền màu đỏ cảnh báo.

### Test Case 5.6: Gửi trình duyệt thành công khi đủ điều kiện
- **Các bước thực hiện:**
  1. Điền đầy đủ Buổi dự, GV1, GV2 cho toàn bộ các lớp.
  2. Bấm **Gửi trình duyệt →**.
  3. Tại modal xác nhận, bấm **Xác nhận gửi**.
- **Kết quả mong đợi:**
  - Hệ thống gửi thành công, chuyển hướng về trang Quản lý KH dự giờ.
  - Kế hoạch chuyển sang trạng thái badge xanh dương **Chờ duyệt (SUBMITTED)**.
  - Chuông thông báo của TBĐT nhận được thông báo kế hoạch mới.

---

## KỊCH BẢN 6: PHÂN CÔNG DỰ GIỜ CHÉO BỘ MÔN / CƠ SỞ (UC-11 - CNBM, TM)

### Test Case 6.1: Mở Modal tìm kiếm dự giờ chéo
- **Tài khoản:** `dungpt (CNBM CNTT)`
- **Các bước thực hiện:**
  1. Tại ô GV1, GV2 hoặc GV3, bấm vào liên kết **tìm** (hoặc **+ Tìm gán**).
- **Kết quả mong đợi:**
  - Modal tìm kiếm mở ra với tiêu đề: *"Phân công người dự giờ chéo (Bộ môn / Cơ sở)"*.
  - **Luôn hiển thị sẵn toàn bộ danh sách giảng viên trên hệ thống** (không bắt buộc phải gõ mới hiện).
  - Giảng viên cùng cơ sở (Đà Nẵng) được ưu tiên xếp đầu với badge **Cùng cơ sở**.
  - Giảng viên cơ sở khác (Hà Nội, TP.HCM...) có badge **Khác cơ sở**.

### Test Case 6.2: Tìm kiếm nhanh theo từ khóa
- **Các bước thực hiện:**
  1. Gõ `nhung` vào ô tìm kiếm nhanh trong modal.
- **Kết quả mong đợi:**
  - Danh sách lọc ngay lập tức: hiển thị cả `nhungpt4 (Đà Nẵng)` và `nhungpt_hn (Hà Nội)`.
  - Hiển thị rõ định dạng chuẩn: `Account – Bộ môn – Cơ sở`.

### Test Case 6.3: Chọn giảng viên dự chéo
- **Các bước thực hiện:**
  1. Bấm nút **Chọn** tại dòng `nhungpt_hn` (GV Cơ bản - Hà Nội).
- **Kết quả mong đợi:**
  - Modal tự động đóng lại.
  - Ô GV tương ứng nhận giá trị `nhungpt_hn`.

---

## KỊCH BẢN 7: LẬP KẾ HOẠCH DỰ GIỜ ĐỘT XUẤT (UC-12 - CNBM, TM)

### Test Case 7.1: Mở màn hình Lập KH dự giờ đột xuất
- **Tài khoản:** `dungpt (CNBM)` hoặc `haidv (TM)`
- **Các bước thực hiện:**
  1. Vào menu **Quản lý KH dự giờ** (`/plans`).
  2. Bấm nút đỏ viền nổi bật **+ KH dự giờ đột xuất**.
- **Kết quả mong đợi:**
  - Chuyển sang màn hình `/plans/unscheduled` với tiêu đề màu đỏ *"Lập kế hoạch dự giờ đột xuất (Mã -DX)"*.
  - Banner hướng dẫn quy tắc: áp dụng khi có phản ánh hoặc kiểm tra đột xuất; giảng viên được dự vẫn tính vào chỉ tiêu 15%.

### Test Case 7.2: Hiển thị danh sách lớp gom theo Giảng viên
- **Các bước thực hiện:**
  1. Quan sát bảng danh sách lớp.
- **Kết quả mong đợi:**
  - Bảng hiển thị toàn bộ các lớp của bộ môn trong block.
  - Phân tách theo từng giảng viên bằng thanh tiêu đề mỏng có avatar, tên, mã GV, tổng số lớp và nút `+ Chọn cả N lớp`.

### Test Case 7.3: Ràng buộc bắt buộc nhập lý do đột xuất
- **Các bước thực hiện:**
  1. Tích chọn 1 lớp bất kỳ.
  2. Không nhập lý do vào ô text.
  3. Bấm **Tiếp tục phân công người dự →**.
- **Kết quả mong đợi:**
  - Hệ thống chặn lại và cảnh báo: *"Vui lòng điền đủ lý do đột xuất cho các lớp đã chọn!"*.
  - Ô lý do chuyển viền đỏ.

### Test Case 7.4: Phân công và gửi duyệt KH đột xuất
- **Các bước thực hiện:**
  1. Nhập lý do: *"Sinh viên phản ánh tốc độ giảng bài nhanh, kiểm tra đột xuất"*.
  2. Bấm **Tiếp tục phân công người dự →**.
  3. Chọn buổi dạy thật, gán đủ GV1 và GV2.
  4. Bấm **Gửi duyệt KH đột xuất (-DX) →**.
- **Kết quả mong đợi:**
  - Kế hoạch được tạo với mã tự động có hậu tố `-DX` (VD: `KH-CNTT-FALL2026-B1-DX`).
  - Kế hoạch chuyển sang trạng thái **Chờ duyệt** và đi vào luồng duyệt riêng.

---

## KỊCH BẢN 8: LUỒNG PHÊ DUYỆT & VÒNG ĐỜI KẾ HOẠCH (UC-13 - TBDT, CNBM, HEAD_OFFICE)

### Test Case 8.1: TBĐT xem chi tiết và Phê duyệt kế hoạch (`APPROVED`)
- **Tài khoản:** `loanth (TBDT)`
- **Các bước thực hiện:**
  1. Vào trang **Quản lý KH dự giờ** (`/plans`), tìm kế hoạch có trạng thái `SUBMITTED`.
  2. Bấm nút **Xem** (`/plans/:id`).
  3. Thanh hành động của TBĐT cố định ở đáy màn hình hiển thị 3 nút: **Từ chối**, **Yêu cầu sửa**, **Phê duyệt kế hoạch**.
  4. Bấm nút xanh **✓ Phê duyệt kế hoạch** → Xác nhận duyệt.
- **Kết quả mong đợi:**
  - Trạng thái kế hoạch chuyển sang badge xanh lá **ĐÃ DUYỆT (APPROVED)**.
  - Lịch sử phê duyệt (Timeline) ghi nhận sự kiện: *"Phê duyệt kế hoạch bởi Hoàng Thị Loan (loanth)"*.

### Test Case 8.2: TBĐT Yêu cầu chỉnh sửa kế hoạch (`REJECTED_NEED_EDIT`)
- **Tài khoản:** `loanth (TBDT)`
- **Các bước thực hiện:**
  1. Mở chi tiết 1 kế hoạch đang ở trạng thái `SUBMITTED`.
  2. Bấm nút **✎ Yêu cầu sửa**.
  3. Hộp thoại yêu cầu nhập ghi chú phản hồi mở ra.
  4. Thử để trống và bấm gửi -> Nút bị vô hiệu hóa (bắt buộc nhập).
  5. Nhập nội dung: *"Đổi GV2 ở lớp CP18301 sang giảng viên cơ bản khác"* và bấm **Gửi yêu cầu sửa**.
- **Kết quả mong đợi:**
  - Kế hoạch chuyển sang badge đỏ **YÊU CẦU SỬA (REJECTED_NEED_EDIT)**.
  - Ghi chú được lưu vào dòng lịch sử và hiển thị thành banner cảnh báo màu vàng ở đầu trang cho CNBM đọc.

### Test Case 8.3: CNBM thực hiện chỉnh sửa lại kế hoạch bị yêu cầu sửa
- **Tài khoản:** `dungpt (CNBM)`
- **Các bước thực hiện:**
  1. Vào **Quản lý KH dự giờ** (`/plans`).
  2. Tìm kế hoạch đang ở trạng thái `REJECTED_NEED_EDIT`.
  3. Bấm nút **Sửa** (nút viền xanh).
  4. Điều chỉnh lại thông tin theo yêu cầu của TBĐT.
  5. Bấm **Gửi trình duyệt (SUBMITTED)**.
- **Kết quả mong đợi:**
  - Kế hoạch chuyển lại về trạng thái **Chờ duyệt (SUBMITTED)** để TBĐT thẩm định lại.

### Test Case 8.4: TBĐT Từ chối kế hoạch (`CANCELLED`)
- **Tài khoản:** `loanth (TBDT)`
- **Các bước thực hiện:**
  1. Mở 1 kế hoạch đang chờ duyệt.
  2. Bấm nút **✕ Từ chối**.
  3. Nhập lý do từ chối: *"Kỳ này không tổ chức dự giờ môn này"* và bấm xác nhận.
- **Kết quả mong đợi:**
  - Kế hoạch chuyển sang trạng thái badge xám **ĐÃ TỪ CHỐI (CANCELLED)**.

### Test Case 8.5: Kiểm tra nút Xóa (Nút viền đỏ đồng bộ giao diện)
- **Tài khoản:** `dungpt (CNBM)`
- **Các bước thực hiện:**
  1. Tại danh sách kế hoạch, quan sát cột Thao tác.
- **Kết quả mong đợi:**
  - Nút Xóa hiển thị dạng **nút bấm viền đỏ** chữ "Xóa" (`btn-outline-danger`), thẳng hàng và đồng bộ với nút "Sửa" và "Xem" (không còn icon thùng rác cũ).

### Test Case 8.6: Kiểm tra cơ chế Xóa an toàn đối với Kế hoạch đã duyệt (`APPROVED`)
- **Tài khoản:** `dungpt (CNBM)` hoặc `loanth (TBDT)`
- **Các bước thực hiện:**
  1. Tìm một kế hoạch đã có trạng thái `APPROVED`.
  2. Quan sát nút Xóa của kế hoạch đó.
- **Kết quả mong đợi:**
  - Nút Xóa **bị vô hiệu hóa (disabled)** đối với CNBM và TBDT.
  - Di chuột vào nút hiện tooltip: *"Kế hoạch đã duyệt cần Super Admin đồng ý để xóa"*.

### Test Case 8.7: Super Admin (`HEAD_OFFICE`) xóa kế hoạch đã duyệt với Modal cảnh báo mạnh
- **Tài khoản:** `ho_admin (HEAD_OFFICE)`
- **Các bước thực hiện:**
  1. Tìm kế hoạch đã có trạng thái `APPROVED`.
  2. Bấm nút **Xóa** (nút không bị vô hiệu hóa với Super Admin).
- **Kết quả mong đợi:**
  - Hộp thoại cảnh báo mạnh viền đỏ mở ra:
    - Hiển thị rõ mã kế hoạch cần xóa.
    - Dòng cảnh báo: *"⚠️ Xóa không khôi phục được: Mọi buổi dự giờ và toàn bộ phiếu đánh giá liên quan sẽ bị mất hoàn toàn!"*.
  3. Bấm **Xác nhận xóa vĩnh viễn** -> Kế hoạch được xóa khỏi hệ thống.
