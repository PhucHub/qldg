// Mock data phục vụ chạy độc lập khi chưa bật backend hoặc demo nhanh
export const MOCK_USERS = [
  { id: 1, account: 'loanth', fullName: 'Hoàng Thị Loan', role: 'TBDT', staffCode: '00246900', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: '', label: 'loanth - Hoàng Thị Loan (TBDT, DNA)' },
  { id: 2, account: 'tuanna', fullName: 'Nguyễn Anh Tuấn', role: 'TCQLDT', staffCode: '00246901', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: '', label: 'tuanna - Nguyễn Anh Tuấn (TCQLDT, DNA)' },
  { id: 3, account: 'dungpt', fullName: 'Phan Tiến Dũng', role: 'CNBM', staffCode: '00246910', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'dungpt - Phan Tiến Dũng (CNBM CNTT, DNA)' },
  { id: 4, account: 'haidv', fullName: 'Đỗ Văn Hải', role: 'TM', staffCode: '00246911', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'haidv - Đỗ Văn Hải (TM CNTT, DNA)' },
  { id: 5, account: 'nhungpt4', fullName: 'Phạm Thị Nhung', role: 'CNBM', staffCode: '00246920', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CB', label: 'nhungpt4 - Phạm Thị Nhung (CNBM Nhóm CB, DNA)' },
  { id: 6, account: 'namnv', fullName: 'Nguyễn Văn Nam', role: 'GV', staffCode: '00246931', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'namnv - Nguyễn Văn Nam (GV CNTT, DNA)' },
  { id: 7, account: 'ho_admin', fullName: 'Nguyễn Văn Hùng', role: 'HEAD_OFFICE', staffCode: '00100001', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: '', headOffice: true, label: 'ho_admin - Nguyễn Văn Hùng (Super Admin)' }
]

export const MOCK_SEMESTERS = [
  { id: 1, term: 'FALL', year: 2026, name: 'Fall 2026', shortCode: 'FA26', ordinal: 6080, startDate: '2026-09-01', block1End: '2026-10-25', block2Start: '2026-10-26', endDate: '2026-12-20' },
  { id: 2, term: 'SUMMER', year: 2026, name: 'Summer 2026', shortCode: 'SU26', ordinal: 6079, startDate: '2026-05-02', block1End: '2026-06-25', block2Start: '2026-06-26', endDate: '2026-08-25' },
  { id: 3, term: 'SPRING', year: 2026, name: 'Spring 2026', shortCode: 'SP26', ordinal: 6078, startDate: '2026-01-05', block1End: '2026-02-28', block2Start: '2026-03-01', endDate: '2026-04-30' }
]

export const MOCK_FACULTIES = [
  { id: 1, code: 'CNTT', name: 'Công nghệ thông tin' },
  { id: 2, code: 'CB', name: 'Cơ bản' },
  { id: 3, code: 'TA', name: 'Tiếng Anh' },
  { id: 4, code: 'TT', name: 'Tiếng Trung' },
  { id: 5, code: 'TKDH', name: 'Thiết kế đồ họa' }
]

export const MOCK_JOBS = [
  {
    id: 1,
    fileName: 'Lich_Giang_Day_Fall2026_B1_v1.xlsx',
    fileSize: 425120,
    status: 'DONE',
    totalRows: 1420,
    inserted: 1200,
    updated: 180,
    skipped: 40,
    errors: 0,
    progress: 100,
    message: 'Thành công: 1200, Cập nhật: 180, Bỏ qua: 40, Lỗi: 0',
    createdAt: '2026-10-15T09:30:00',
    finishedAt: '2026-10-15T09:30:45',
    uploader: { fullName: 'Nguyễn Anh Tuấn', account: 'tuanna' }
  },
  {
    id: 2,
    fileName: 'LichDay_BoSung_CNTT_v2.xlsx',
    fileSize: 185600,
    status: 'DONE_WITH_ERRORS',
    totalRows: 250,
    inserted: 190,
    updated: 35,
    skipped: 15,
    errors: 10,
    progress: 100,
    message: 'Thành công: 190, Cập nhật: 35, Bỏ qua: 15, Lỗi: 10',
    createdAt: '2026-10-16T14:15:00',
    finishedAt: '2026-10-16T14:15:20',
    uploader: { fullName: 'Nguyễn Anh Tuấn', account: 'tuanna' }
  }
]

export const MOCK_JOB_ROWS = [
  { id: 1, rowNo: 2, classCode: 'WD18301', subjectCode: 'COM108', teacherAccount: 'namnv', sessionNo: 1, result: 'SKIPPED', oldValue: '2026-10-20 Ca 2 F201', message: 'Trùng hoàn toàn buổi học đã có' },
  { id: 2, rowNo: 3, classCode: 'WD18301', subjectCode: 'COM108', teacherAccount: 'namnv', sessionNo: 2, result: 'UPDATED_ROOM', oldValue: 'F201', newValue: 'B203', message: 'Đổi phòng từ F201 sang B203' },
  { id: 3, rowNo: 4, classCode: 'WD18301', subjectCode: 'COM108', teacherAccount: 'namnv', sessionNo: 3, result: 'UPDATED_TIME', oldValue: '2026-10-24 Ca 2', newValue: '2026-10-25 Ca 3', message: 'Đổi ngày và ca học' },
  { id: 4, rowNo: 5, classCode: 'CP18301', subjectCode: 'COM1081', teacherAccount: 'linhdt', sessionNo: 1, result: 'INSERTED', newValue: '2026-10-21 (Ca 3, F202)', message: 'Thêm mới buổi học' },
  { id: 5, rowNo: 6, classCode: 'IT18399', subjectCode: 'COM999', teacherAccount: 'unknowngv', sessionNo: 1, result: 'ERROR', message: 'Không tìm thấy giảng viên unknowngv tại cơ sở' }
]

export const MOCK_JOB_ALERTS = [
  { id: 1, subjectCode: 'COM1081', subjectName: 'Nhập môn lập trình nâng cao', kind: 'UPDATE_VERSION', baseCode: 'COM108', classCount: 4 },
  { id: 2, subjectCode: 'AI2010', subjectName: '[Chưa đặt tên - AI2010]', kind: 'NEW', baseCode: null, classCount: 2 }
]
