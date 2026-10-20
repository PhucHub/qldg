10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
Xin chào, Hoàng Thị Loan (TC&QLĐT - CĐ Đà Nẵng ) Đăng xuất
Tổng quan hệ thống QLDG
Tài liệu bàn giao — mô tả cách hệ thống tổ chức dữ liệu và các luồng nghiệp vụ chính. Cập nhật 07/10/2026.
1. Hệ thống làm gì
QLDG quản lý hoạt động dự giờ của FPT Polytechnic trên 14 cơ sở. Vòng đời một kỳ học diễn ra theo bốn bước:
1. Nạp lịch dạy 2. Lập kế hoạch dự giờ 3. Duyệt kế hoạch 4. Dự giờ & chấm điểm
import file từ hệ thống đào tạo → CNBM / TM chọn lớp, phân công → TBĐT phê duyệt → GV dự giờ nhập phiếu, hệ thống
người dự thống kê
Quy mô dữ liệu thực tế
| Cơ sở               | 14     | Bộ môn (dùng chung toàn hệ thống) | 20        |
| ------------------- | ------ | --------------------------------- | --------- |
| Tài khoản           | 2.550  | Môn học                           | 740       |
| Phân công giảng dạy | 85.405 | Buổi học                          | 1.424.006 |
| Kế hoạch dự giờ     | 1.622  | Buổi dự giờ                       | 6.253     |
| Câu hỏi trên phiếu  | 23     | Lượt chấm từng câu                | 267.122   |
2. Sơ đồ dữ liệu
Cơ sở dữ liệu chia làm hai khối rõ rệt. Khối thứ nhất mô tả ai dạy môn gì, ở đâu, khi nào. Khối thứ hai mô tả ai dự giờ ai và chấm bao nhiêu điểm. Hai khối nối
với nhau qua bảng tài khoản và bảng môn học.
https://qldg.poly.edu.vn/tong-quan 1/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
2.1. Khối giảng dạy
| campuses |     | faculties           |     | semesters        |
| -------- | --- | ------------------- | --- | ---------------- |
| 14 cơ sở |     | 20 bộ môn, toàn cục |     | kỳ + mốc 2 block |
bộ môn nào
môn của bộ môn
cơ sở nào
| campus_faculty      |     | subjects |     |     |
| ------------------- | --- | -------- | --- | --- |
| bật/tắt + group_key |     | 740 môn  |     |     |
dạy môn gì
thuộc cơ sở
| users           | ai dạy | subject_user        | kỳ nào | semesters        |
| --------------- | ------ | ------------------- | ------ | ---------------- |
| 2.550 tài khoản |        | 85.405 — "teaching" |        | kỳ của phân công |
các buổi học
calendars
1.424.006 buổi học
Đọc tên bảng cho đúng.  nghe như bảng nối đơn thuần nhưng thực chất là một phân công giảng dạy — trong mã nguồn gọi là Teaching.
subject_user
Một dòng   = một lớp + một môn + một block + một kỳ + một giảng viên. Mỗi dòng   là một buổi học cụ thể của phân công đó.
subject_user calendars
2.2. Khối dự giờ
https://qldg.poly.edu.vn/tong-quan 2/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
inspections inspection_details evaluations
gồm nhiều buổi điểm từng câu
1.622 kế hoạch 6.253 buổi dự giờ 267.122 lượt chấm
cơ sở + bộ môn + kỳ + block GV được dự + GV1/2/3 1 dòng = 1 câu hỏi
câu nào
teacher_id, inspector_1/2/3
questions
users 23 câu × tối đa 4 điểm
mỗi buổi tham chiếu 4 lần:
người được dự + 3 người dự
Thang điểm một phiếu = số câu × 4. Hiện 23 câu nên tối đa 92 điểm, và mốc 75% tương đương 69 điểm. Hệ thống luôn tính lại từ phiếu chứ không gán
cứng số 69, nên đổi số câu thì mốc tự đổi theo.
3. Mô hình bộ môn theo cơ sở
Đây là phần dễ hiểu sai nhất. Bảng là toàn cục: 20 bộ môn dùng chung cho cả 14 cơ sở, và không có cột cơ sở. Việc "cơ sở nào có bộ môn nào"
faculties
nằm ở bảng nối .
campus_faculty
faculties campuses campus_faculty
20 bộ môn, không gắn cơ sở + 14 cơ sở → is_active: cơ sở có dạy không
group_key: gộp nhóm quản lý
https://qldg.poly.edu.vn/tong-quan 3/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
Hai cột quyết định mọi thứ
 — cơ sở này có đào tạo bộ môn đó không. Tắt thì bộ môn biến mất khỏi mọi màn hình của cơ sở.
is_active
 — tên nhóm. Các bộ môn cùng một   tại cùng một cơ sở hợp thành một nhóm quản lý, do một CNBM phụ trách.
| group_key | group_key |     |     |     |
| --------- | --------- | --- | --- | --- |
group_key  để trống, hoặc chỉ có duy nhất một bộ môn mang khoá đó, thì bộ môn đứng một mình. Và  group_key không liên quan giữa các cơ sở — cùng
tên   ở Hà Nội và Đà Nẵng là hai nhóm khác nhau.
CB
Phạm vi xem của từng vai được tính thế nào
Mọi màn hình đều gọi chung một hàm   để biết người đang đăng nhập được thấy bộ môn nào. Đây là chỗ duy nhất quyết định phạm vi, nên
my_faculties()
sửa quy tắc chỉ cần sửa một nơi:
| Vai                  |     | My_faculties() Trả Về |            |     |
| -------------------- | --- | --------------------- | ---------- | --- |
| TBĐT, TC&QLĐT, TP.ĐT |     | Mọi bộ môn            |  của cơ sở |     |
is_active
| CNBM |     | Cả nhóm theo  |  của bộ môn mình |     |
| ---- | --- | ------------- | ---------------- | --- |
group_key
| Trưởng môn |     | Đúng bộ môn mình |     |     |
| ---------- | --- | ---------------- | --- | --- |
| Giảng viên |     | Bộ môn của mình  |     |     |
Bộ môn sở hữu môn nhưng không có biên chế. Có bộ môn không ai mang nó trong hồ sơ  , nhưng môn học của nó vẫn được dạy bởi
users.faculty_id
giảng viên bộ môn khác — ví dụ Ứng dụng phần mềm ở Đà Nẵng có 0 người biên chế nhưng 15 giảng viên dạy môn của nó. Vì vậy mọi thống kê trong hệ
thống đếm theo giảng viên có lớp dạy (qua   +  ), chứ không đếm theo  .
|     | subject_user | subjects.faculty_id |     | users.faculty_id |
| --- | ------------ | ------------------- | --- | ---------------- |
4. Luồng import lịch dạy
TC&QLĐT tải file lịch lên. File không được xử lý ngay mà xếp vào hàng đợi, một tiến trình nền chạy theo lịch sẽ lấy ra xử lý và gửi thông báo kết quả.
https://qldg.poly.edu.vn/tong-quan 4/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
1. Tải file lên 2. Vào hàng đợi 3. Tiến trình nền xử lý 4. Thông báo kết quả
→ → →
/calendar/import queue_tasks job:import-calendars kèm danh sách dòng lỗi
Mỗi dòng file được xử lý thế nào
1. Đọc ngày, tìm xem rơi vào kỳ nào.
2. Tìm giảng viên theo tài khoản, tìm môn theo mã môn. Mã môn chưa có thì tự tạo môn mới và báo TC&QLĐT vào đặt tên.
3. Tìm hoặc tạo phân công giảng dạy (lớp + môn + block + kỳ + giảng viên).
4. Ghi buổi học vào .
calendars
Nhận diện một buổi học — điểm cốt lõi
Danh tính ổn định của một buổi học là số thứ tự buổi trong phạm vi phân công, chứ không phải ngày. Ngày có thể đổi; "buổi 5" thì vẫn là buổi 5.
1. Tìm theo số thứ tự 2. Chưa có số thì tìm theo ngày + ca 3. Không khớp gì thì mới là buổi mới
→ →
(phân công, số thứ tự buổi) dữ liệu cũ và đường API
Hệ quả khi import lại cùng một file:
Tình Huống Hệ Thống Làm Gì
Trùng hoàn toàn Bỏ qua, không tạo bản sao
Đổi phòng Cập nhật phòng, vẫn một buổi
Đổi ngày Cập nhật ngày tại chỗ, không sinh buổi mồ côi
Hai buổi hoán ngày cho nhau Tự xử lý qua một bước trung gian rồi áp giá trị thật
https://qldg.poly.edu.vn/tong-quan 5/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
Ngày học lưu kiểu DATE (không có giờ) — giờ học chính là ca. Và cơ sở dữ liệu có ràng buộc duy nhất trên (phân công, giảng viên, ngày, ca, phòng), nên
dù code có lỗi thì tầng dữ liệu vẫn chặn được trùng lặp.
5. Cách liệt kê lớp khi Duyệt
Khi CNBM chọn kỳ / block / bộ môn rồi bấm Duyệt, hệ thống lấy toàn bộ lớp học phần của bộ môn trong block đó (chỉ lớp có lịch dạy, và giảng viên cùng cơ
sở), rồi chia làm ba nhóm:
Bắt buộc dự giờ Các lớp khác Đã thuộc kế hoạch
dạy môn mới, hoặc 6 kỳ liên tiếp không dạy rồi dạy lại chưa thuộc kế hoạch nào — thêm tay nếu muốn của block này rồi
"Dạy môn mới" được xác định thế nào
Hệ thống tìm kỳ gần nhất mà cặp (giảng viên, môn) đã từng dạy, so với kỳ hiện tại. Khoảng cách đếm theo số thứ tự kỳ chứ không theo tháng — vì ranh giới
kỳ không đều nhau và còn phụ thuộc block.
Trường Hợp Xếp Vào
Chưa từng dạy môn này Bắt buộc — Lần đầu dạy
Nghỉ dạy môn đó 6 kỳ liên tiếp, kỳ thứ 7 dạy lại Bắt buộc — Khác
Đã dạy gần đây Các lớp khác (không bắt buộc)
Mã môn bản cập nhật được coi là môn khác. là một môn riêng so với , nên giảng viên dạy bản mới sẽ bị xếp vào diện lần đầu dạy. Đây là
COM1081 COM108
hành vi có chủ đích: nội dung môn đã đổi.
Dự giờ đột xuất đi đường khác
https://qldg.poly.edu.vn/tong-quan 6/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
Màn hình đột xuất không phân loại ba nhóm — nó hiện tất cả lớp, kể cả lớp đã có kế hoạch, vì dự giờ đột xuất đúng nghĩa là dự một lớp bất kỳ. Kế hoạch đột
xuất được lưu thành bản riêng (mã có đuôi ) và đi duyệt độc lập.
-DX
6. Logic chọn giảng viên đi dự giờ
Mỗi buổi dự giờ có tối đa ba người dự: GV1 và GV2 bắt buộc, GV3 tuỳ chọn.
Danh sách dựng sẵn Nút “+” tìm thêm
+
giảng viên trong phạm vi bộ môn toàn hệ thống, kể cả cơ sở khác
Dự giờ chéo bộ môn và chéo cơ sở đều được phép, nên ô tìm thêm quét toàn hệ thống, chỉ xếp người cùng cơ sở lên đầu cho tiện. Nhãn hiển thị dạng
account – bộ môn – cơ sở để không chọn nhầm người trùng tên.
Ngày dự giờ chọn từ đâu
Chọn lớp xong, hệ thống đổ ra danh sách buổi học thật của lớp đó để chọn ngày — nên không thể đặt lịch dự giờ vào ngày lớp không học. Danh sách chỉ lấy
ca 1–6 (ca học thật, loại bỏ ca tự học online) và giới hạn trong khoảng đầu của phân công.
Ai xem được phiếu đánh giá
Vai Xem Được
Giảng viên được dự giờ Tất cả phiếu chấm mình
CNBM / TM Phiếu của giảng viên trong phạm vi quản lý
TBĐT / TC&QLĐT Tất cả phiếu của cơ sở
GV đi dự giờ Chỉ phiếu do chính mình nhập
https://qldg.poly.edu.vn/tong-quan 7/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
Cảnh báo quá hạn
Một buổi bị coi là quá hạn khi đã qua ngày dự kiến (hoặc ngày thực tế nếu có) 72 giờ mà vẫn còn GV1/GV2/GV3 chưa nhập phiếu, và kế hoạch đã được
duyệt. Con số cảnh báo luôn tính theo đúng bộ lọc kỳ / block / bộ môn đang chọn, và theo phạm vi quản lý của người xem.
7. Logic báo cáo
Báo cáo tính trực tiếp từ dữ liệu gốc mỗi lần mở trang, không đọc số liệu dựng sẵn. Lý do: bộ đếm cộng dồn không có đường trừ nên duyệt lại một kế hoạch
hay xoá kế hoạch là số liệu trôi khỏi sự thật; dữ liệu lại đủ nhỏ để tính lại tức thì.
Mẫu số: ai được tính là "giảng viên của bộ môn"
KHÔNG dùng Dùng
→
users.faculty_id (biên chế) giảng viên CÓ LỚP DẠY môn của bộ môn trong block đó
Cách này sửa được trường hợp bộ môn không có biên chế, đồng thời cho lịch sử chính xác cho mọi kỳ cũ mà không cần bảng lưu ảnh chụp — vì phân công
giảng dạy vốn đã ghi theo từng kỳ và từng block.
Các chỉ số
https://qldg.poly.edu.vn/tong-quan 8/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
Chỉ Số Cách Tính
Tổng số GV Giảng viên phân biệt có lớp dạy môn của bộ môn, trong block
Cần được dự giờ làm tròn lên(T̉ông GV × 15%)
Đã có trong KH Giảng viên phân biệt trong kế hoạch đã duyệt / hoàn thành
Đã được dự giờ Buổi đủ phiếu: GV1 và GV2 đã nộp, và GV3 nếu có phân công
Số lượt dự giờ Số ô GV1/2/3 đã nộp phiếu — công việc thực tế
GV ≤ 75% thang điểm Trung bình tỉ lệ điểm của các GV chấm ≤ 75%
Dòng Tổng hợp và dòng Cả kỳ không bằng tổng các dòng trên. Chúng đếm giảng viên phân biệt: một người được dự giờ ở cả hai block chỉ tính một
lần trong dòng Cả kỳ. Ví dụ thật: Block 1 có 30 GV, Block 2 có 25 GV, nhưng cả kỳ chỉ 42 chứ không phải 55 — vì 13 người được dự giờ ở cả hai block.
Phạm vi xem
Giống mọi màn hình khác, lấy từ . Ô bộ môn để trống là xem tất cả trong phạm vi: bảng hiện từng bộ môn, cộng dòng Tổng hợp mỗi block và
my_faculties()
mục Tổng hợp cả kỳ.
8. Những cái bẫy khi bảo trì
Những điều không nhìn ra được từ mã nguồn, nhưng sai là hỏng dữ liệu:
Xoá mềm không giải phóng ràng buộc duy nhất
MySQL/MariaDB áp ràng buộc duy nhất lên cả dòng đã xoá mềm. Dọn trùng lặp bằng cách xoá mềm là chưa đủ — dòng đã xoá mềm vẫn chiếm chỗ và lần
ghi sau sẽ bị chặn. Phải xoá cứng những dòng đang che chỗ.
https://qldg.poly.edu.vn/tong-quan 9/10

10/7/26, 12:59 AM Tổng quan hệ thống | Trường Cao đẳng FPT Polytechnic
Một người có nhiều tài khoản là đúng thiết kế
Đăng nhập là cặp email + cơ sở. Một người làm việc ở nhiều cơ sở thì có nhiều dòng tài khoản cùng email — đừng gộp lại. Quyền Head Office vì vậy gắn với
email, không gắn với từng dòng.
Phân quyền nằm trong controller, không nằm ở route
Toàn bộ route chỉ gắn middleware đã đăng nhập. Việc kiểm vai nằm rải trong từng controller, nên thêm controller mới mà quên kiểm vai là hở hoàn toàn. Ẩn
nút ở giao diện không phải là biện pháp bảo vệ.
Vai Admin đi qua mọi cửa
Hàm kiểm vai trả về đúng cho vai Admin ở mọi phép kiểm. Vai này không dùng trong vận hành; nếu thấy tài khoản nào mang vai Admin thì gần như chắc
chắn là nhập nhầm.
Dự án không có quản lý phiên bản
Mã nguồn trên máy chủ không được quản lý bằng git: không rollback được, không truy được ai đổi gì. Đây là rủi ro lớn nhất khi tiếp quản. Vì vậy nhiều
quyết định thiết kế được ghi thành chú thích dài ngay trong mã nguồn — đó là nơi duy nhất tra được lý do. Đừng xoá những chú thích đó khi dọn
code.
Tài liệu kỹ thuật chi tiết hơn về phần lịch dạy nằm ở trong mã nguồn. Hướng dẫn thao tác cho người dùng cuối ở Hướng dẫn sử dụng.
docs/LichDay.md
https://qldg.poly.edu.vn/tong-quan 10/10