import * as XLSX from 'xlsx'

// ==========================================
// MOCK USERS (+ 5 GIÁO VIÊN MỚI ĐƯỢC THÊM)
// ==========================================
export const MOCK_USERS = [
  { id: 1, account: 'loanth', fullName: 'Hoàng Thị Loan', role: 'TBDT', staffCode: '00246900', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: '', label: 'loanth - Hoàng Thị Loan (TBDT, DNA)' },
  { id: 2, account: 'tuanna', fullName: 'Nguyễn Anh Tuấn', role: 'TCQLDT', staffCode: '00246901', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: '', label: 'tuanna - Nguyễn Anh Tuấn (TCQLDT, DNA)' },
  { id: 3, account: 'dungpt', fullName: 'Phan Tiến Dũng', role: 'CNBM', staffCode: '00246910', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'dungpt - Phan Tiến Dũng (CNBM CNTT, DNA)' },
  { id: 4, account: 'haidv', fullName: 'Đỗ Văn Hải', role: 'TM', staffCode: '00246911', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'haidv - Đỗ Văn Hải (TM CNTT, DNA)' },
  { id: 5, account: 'nhungpt4', fullName: 'Phạm Thị Nhung', role: 'CNBM', staffCode: '00246920', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CB', label: 'nhungpt4 - Phạm Thị Nhung (CNBM Nhóm CB, DNA)' },
  { id: 6, account: 'namnv', fullName: 'Nguyễn Văn Nam', role: 'GV', staffCode: '00246931', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'namnv - Nguyễn Văn Nam (GV CNTT, DNA)' },
  { id: 7, account: 'ho_admin', fullName: 'Nguyễn Văn Hùng', role: 'HEAD_OFFICE', staffCode: '00100001', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: '', headOffice: true, label: 'ho_admin - Nguyễn Văn Hùng (Super Admin)' },
  // 5 Giáo viên mới bổ sung:
  { id: 8, account: 'linhdt', fullName: 'Đặng Thùy Linh', role: 'GV', staffCode: '00246932', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'linhdt - Đặng Thùy Linh (GV CNTT, DNA)' },
  { id: 9, account: 'tunglt', fullName: 'Lê Thanh Tùng', role: 'GV', staffCode: '00246933', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'tunglt - Lê Thanh Tùng (GV CNTT, DNA)' },
  { id: 10, account: 'hah7', fullName: 'Hoàng Anh Huy', role: 'GV', staffCode: '00246934', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CNTT', label: 'hah7 - Hoàng Anh Huy (GV CNTT, DNA)' },
  { id: 11, account: 'huongnt', fullName: 'Nguyễn Thị Hương', role: 'GV', staffCode: '00246935', campusCode: 'dna', campusName: 'CĐ FPT Polytechnic Đà Nẵng', facultyCode: 'CB', label: 'huongnt - Nguyễn Thị Hương (GV Nhóm CB, DNA)' },
  { id: 12, account: 'minhtv', fullName: 'Trần Văn Minh', role: 'GV', staffCode: '00388902', campusCode: 'hcm', campusName: 'CĐ FPT Polytechnic TP.HCM', facultyCode: 'CNTT', label: 'minhtv - Trần Văn Minh (GV CNTT, TP.HCM)' }
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
  { id: 5, code: 'TKDH', name: 'Thiết kế đồ họa' },
  { id: 6, code: 'QTKD', name: 'Quản trị kinh doanh' }
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

// ==========================================
// DỮ LIỆU MẪU KẾ HOẠCH DỰ GIỜ (MOCK PLANS)
// ==========================================
export const INITIAL_MOCK_PLANS = [
  {
    id: 1,
    code: 'KH-CNTT-FA26-B1-001',
    semesterId: 1,
    semesterName: 'Fall 2026',
    block: 1,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin',
    creatorAccount: 'dungpt',
    creatorName: 'Phan Tiến Dũng',
    classCount: 2,
    status: 'SUBMITTED',
    unscheduled: false,
    createdAt: '2026-10-15T08:30:00',
    submittedAt: '2026-10-16T09:00:00',
    lastNote: 'Kính gửi Trưởng ban đào tạo phê duyệt kế hoạch đợt 1',
    items: [
      {
        id: 101,
        teachingId: 1,
        classCode: 'WD18301',
        subjectCode: 'COM108',
        subjectName: 'Nhập môn lập trình',
        teacherAccount: 'namnv',
        teacherName: 'Nguyễn Văn Nam',
        plannedDate: '2026-10-22',
        shift: 2,
        inspector1: { account: 'dungpt', fullName: 'Phan Tiến Dũng', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector2: { account: 'haidv', fullName: 'Đỗ Văn Hải', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector3: null,
        reason: 'Lần đầu dạy môn này'
      },
      {
        id: 102,
        teachingId: 2,
        classCode: 'CP18301',
        subjectCode: 'PRO2113',
        subjectName: 'Dự án tốt nghiệp (CNTT)',
        teacherAccount: 'linhdt',
        teacherName: 'Đặng Thùy Linh',
        plannedDate: '2026-10-23',
        shift: 3,
        inspector1: { account: 'dungpt', fullName: 'Phan Tiến Dũng', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector2: { account: 'namnv', fullName: 'Nguyễn Văn Nam', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector3: null,
        reason: 'Môn đồ án'
      }
    ],
    histories: [
      { id: 1, action: 'CREATE_DRAFT', actorName: 'Phan Tiến Dũng', note: 'Tạo bản nháp kế hoạch', createdAt: '2026-10-15T08:30:00' },
      { id: 2, action: 'SUBMIT', actorName: 'Phan Tiến Dũng', note: 'Kính gửi Trưởng ban đào tạo phê duyệt', createdAt: '2026-10-16T09:00:00' }
    ]
  },
  {
    id: 2,
    code: 'KH-CNTT-FA26-B1-002',
    semesterId: 1,
    semesterName: 'Fall 2026',
    block: 1,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin',
    creatorAccount: 'haidv',
    creatorName: 'Đỗ Văn Hải',
    classCount: 1,
    status: 'APPROVED',
    unscheduled: false,
    createdAt: '2026-10-12T10:00:00',
    submittedAt: '2026-10-13T14:00:00',
    lastNote: 'Đã phê duyệt đầy đủ các tiêu chí',
    items: [
      {
        id: 201,
        teachingId: 3,
        classCode: 'IT18302',
        subjectCode: 'MOB1014',
        subjectName: 'Lập trình Java 1',
        teacherAccount: 'tunglt',
        teacherName: 'Lê Thanh Tùng',
        plannedDate: '2026-10-20',
        shift: 1,
        inspector1: { account: 'haidv', fullName: 'Đỗ Văn Hải', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector2: { account: 'namnv', fullName: 'Nguyễn Văn Nam', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector3: null,
        reason: 'Chưa từng dạy'
      }
    ],
    histories: [
      { id: 1, action: 'CREATE_DRAFT', actorName: 'Đỗ Văn Hải', note: 'Khởi tạo kế hoạch nhóm lập trình Java', createdAt: '2026-10-12T10:00:00' },
      { id: 2, action: 'SUBMIT', actorName: 'Đỗ Văn Hải', note: 'Gửi phê duyệt', createdAt: '2026-10-13T14:00:00' },
      { id: 3, action: 'APPROVE', actorName: 'Hoàng Thị Loan', note: 'Đã duyệt kế hoạch', createdAt: '2026-10-14T08:30:00' }
    ]
  },
  {
    id: 3,
    code: 'KH-DX-CNTT-FA26-001',
    semesterId: 1,
    semesterName: 'Fall 2026',
    block: 1,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin',
    creatorAccount: 'dungpt',
    creatorName: 'Phan Tiến Dũng',
    classCount: 1,
    status: 'DRAFT',
    unscheduled: true,
    createdAt: '2026-10-18T11:00:00',
    submittedAt: null,
    lastNote: 'Kế hoạch dự giờ đột xuất kiểm tra nề nếp',
    items: [
      {
        id: 301,
        teachingId: 4,
        classCode: 'IT18303',
        subjectCode: 'WEB1013',
        subjectName: 'Thiết kế Web với HTML5 & CSS3',
        teacherAccount: 'hah7',
        teacherName: 'Hoàng Anh Huy',
        plannedDate: '2026-10-21',
        shift: 4,
        inspector1: { account: 'dungpt', fullName: 'Phan Tiến Dũng', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector2: { account: 'haidv', fullName: 'Đỗ Văn Hải', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector3: null,
        reason: 'Dự giờ đột xuất kiểm tra nề nếp theo yêu cầu TBĐT'
      }
    ],
    histories: [
      { id: 1, action: 'CREATE_DRAFT', actorName: 'Phan Tiến Dũng', note: 'Lập dự thảo đột xuất', createdAt: '2026-10-18T11:00:00' }
    ]
  },
  {
    id: 4,
    code: 'KH-CB-FA26-B1-001',
    semesterId: 1,
    semesterName: 'Fall 2026',
    block: 1,
    facultyId: 2,
    facultyName: 'Cơ bản',
    creatorAccount: 'nhungpt4',
    creatorName: 'Phạm Thị Nhung',
    classCount: 1,
    status: 'APPROVED',
    unscheduled: false,
    createdAt: '2026-10-14T09:00:00',
    submittedAt: '2026-10-14T15:00:00',
    lastNote: 'Đã duyệt toàn bộ',
    items: [
      {
        id: 401,
        teachingId: 5,
        classCode: 'CB18301',
        subjectCode: 'ENG101',
        subjectName: 'Tiếng Anh 1.1',
        teacherAccount: 'huongnt',
        teacherName: 'Nguyễn Thị Hương',
        plannedDate: '2026-10-20',
        shift: 1,
        inspector1: { account: 'nhungpt4', fullName: 'Phạm Thị Nhung', facultyName: 'Cơ bản', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector2: { account: 'nhungpt_hn', fullName: 'Phạm Thu Nhung', facultyName: 'Cơ bản', campusName: 'CĐ FPT Polytechnic Hà Nội' },
        inspector3: null,
        reason: 'Dự giờ định kỳ'
      }
    ],
    histories: [
      { id: 1, action: 'CREATE_DRAFT', actorName: 'Phạm Thị Nhung', note: 'Khởi tạo kế hoạch bộ môn Cơ bản', createdAt: '2026-10-14T09:00:00' },
      { id: 2, action: 'SUBMIT', actorName: 'Phạm Thị Nhung', note: 'Gửi phê duyệt', createdAt: '2026-10-14T15:00:00' },
      { id: 3, action: 'APPROVE', actorName: 'Hoàng Thị Loan', note: 'Duyệt kế hoạch Cơ bản', createdAt: '2026-10-15T10:00:00' }
    ]
  },
  {
    id: 5,
    code: 'KH-CNTT-FA26-B1-003',
    semesterId: 1,
    semesterName: 'Fall 2026',
    block: 1,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin',
    creatorAccount: 'dungpt',
    creatorName: 'Phan Tiến Dũng',
    classCount: 1,
    status: 'REJECTED_NEED_EDIT',
    unscheduled: false,
    createdAt: '2026-10-17T08:00:00',
    submittedAt: '2026-10-17T09:30:00',
    lastNote: 'Cần bổ sung giảng viên dự giờ thứ 2 cùng chuyên môn cho lớp WD18301',
    items: [
      {
        id: 501,
        teachingId: 1,
        classCode: 'WD18301',
        subjectCode: 'COM108',
        subjectName: 'Nhập môn lập trình',
        teacherAccount: 'namnv',
        teacherName: 'Nguyễn Văn Nam',
        plannedDate: '2026-10-24',
        shift: 2,
        inspector1: { account: 'dungpt', fullName: 'Phan Tiến Dũng', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng' },
        inspector2: null,
        inspector3: null,
        reason: 'Cần bổ sung GV 2'
      }
    ],
    histories: [
      { id: 1, action: 'CREATE_DRAFT', actorName: 'Phan Tiến Dũng', note: 'Tạo kế hoạch bổ sung', createdAt: '2026-10-17T08:00:00' },
      { id: 2, action: 'SUBMIT', actorName: 'Phan Tiến Dũng', note: 'Gửi phê duyệt', createdAt: '2026-10-17T09:30:00' },
      { id: 3, action: 'REQUEST_EDIT', actorName: 'Hoàng Thị Loan', note: 'Cần bổ sung giảng viên dự giờ thứ 2 cùng chuyên môn cho lớp WD18301', createdAt: '2026-10-17T14:00:00' }
    ]
  }
]

// ==========================================
// QUẢN LÝ LƯU TRỮ KẾ HOẠCH DỰ GIỜ (LOCAL STORAGE)
// ==========================================
const PLANS_STORAGE_KEY = 'qldg_mock_plans_store'

function getMockStore() {
  try {
    const raw = localStorage.getItem(PLANS_STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch (e) {
    console.error('Lỗi đọc mock store:', e)
  }
  localStorage.setItem(PLANS_STORAGE_KEY, JSON.stringify(INITIAL_MOCK_PLANS))
  return INITIAL_MOCK_PLANS
}

function saveMockStore(plans) {
  try {
    localStorage.setItem(PLANS_STORAGE_KEY, JSON.stringify(plans))
  } catch (e) {
    console.error('Lỗi lưu mock store:', e)
  }
}

export function getMockPlansGrouped({ semesterId, block, facultyId, status } = {}) {
  const all = getMockStore()
  let filtered = [...all]

  if (semesterId) filtered = filtered.filter(p => Number(p.semesterId) === Number(semesterId))
  if (block && Number(block) > 0) filtered = filtered.filter(p => Number(p.block) === Number(block))
  if (facultyId) filtered = filtered.filter(p => Number(p.facultyId) === Number(facultyId))
  if (status && status !== 'ALL') filtered = filtered.filter(p => p.status === status)

  // Gom theo bộ môn
  const grouped = {}
  for (const p of filtered) {
    const facName = p.facultyName || 'Bộ môn chung'
    if (!grouped[facName]) grouped[facName] = []
    grouped[facName].push({
      id: p.id,
      code: p.code,
      facultyId: p.facultyId,
      facultyName: p.facultyName,
      creatorAccount: p.creatorAccount,
      creatorName: p.creatorName,
      classCount: p.items ? p.items.length : (p.classCount || 1),
      status: p.status,
      unscheduled: p.unscheduled,
      createdAt: p.createdAt,
      submittedAt: p.submittedAt,
      lastNote: p.lastNote
    })
  }
  return grouped
}

export function getMockPlanDetail(id) {
  const all = getMockStore()
  const p = all.find(x => x.id === Number(id))
  if (!p) throw new Error('Không tìm thấy kế hoạch dự giờ')
  return p
}

export function saveOrSubmitMockPlan(data, isSubmit = false, planId = null) {
  const all = [...getMockStore()]
  let existingIndex = planId ? all.findIndex(x => x.id === Number(planId)) : -1

  const sem = MOCK_SEMESTERS.find(s => s.id === Number(data.semesterId)) || MOCK_SEMESTERS[0]
  const fac = MOCK_FACULTIES.find(f => f.id === Number(data.facultyId)) || MOCK_FACULTIES[0]

  const mappedItems = (data.items || []).map((it, idx) => ({
    id: it.id || Date.now() + idx,
    teachingId: it.teachingId || idx + 1,
    classCode: it.classCode || 'Lớp ' + (idx + 1),
    subjectCode: it.subjectCode || 'MH00' + (idx + 1),
    subjectName: it.subjectName || 'Môn học mẫu',
    teacherAccount: it.teacherAccount || 'namnv',
    teacherName: it.teacherName || 'Nguyễn Văn Nam',
    plannedDate: it.plannedDate || '2026-10-25',
    shift: it.shift || 2,
    inspector1: it.inspector1 || { account: 'dungpt', fullName: 'Phan Tiến Dũng', facultyName: fac.name, campusName: 'Đà Nẵng' },
    inspector2: it.inspector2 || null,
    inspector3: it.inspector3 || null,
    reason: it.reason || ''
  }))

  const status = isSubmit ? 'SUBMITTED' : 'DRAFT'
  const now = new Date().toISOString()

  if (existingIndex >= 0) {
    const p = all[existingIndex]
    p.items = mappedItems
    p.classCount = mappedItems.length
    p.status = status
    if (isSubmit) p.submittedAt = now
    p.histories = p.histories || []
    p.histories.push({
      id: Date.now(),
      action: isSubmit ? 'SUBMIT' : 'UPDATE_DRAFT',
      actorName: 'Người dùng',
      note: isSubmit ? 'Trình duyệt kế hoạch' : 'Cập nhật bản nháp',
      createdAt: now
    })
    saveMockStore(all)
    return p
  } else {
    const newId = all.length > 0 ? Math.max(...all.map(x => x.id)) + 1 : 1
    const prefix = data.unscheduled ? 'KH-DX' : 'KH'
    const newCode = `${prefix}-${fac.code}-${sem.shortCode}-B${data.block || 1}-${String(newId).padStart(3, '0')}`
    const newPlan = {
      id: newId,
      code: newCode,
      semesterId: sem.id,
      semesterName: sem.name,
      block: data.block || 1,
      facultyId: fac.id,
      facultyName: fac.name,
      creatorAccount: 'dungpt',
      creatorName: 'Phan Tiến Dũng',
      classCount: mappedItems.length,
      status,
      unscheduled: !!data.unscheduled,
      createdAt: now,
      submittedAt: isSubmit ? now : null,
      lastNote: isSubmit ? 'Kính gửi Trưởng ban Đào tạo phê duyệt' : 'Bản nháp kế hoạch',
      items: mappedItems,
      histories: [
        {
          id: Date.now(),
          action: isSubmit ? 'SUBMIT' : 'CREATE_DRAFT',
          actorName: 'Phan Tiến Dũng',
          note: isSubmit ? 'Tạo mới và gửi phê duyệt' : 'Khởi tạo bản nháp',
          createdAt: now
        }
      ]
    }
    all.unshift(newPlan)
    saveMockStore(all)
    return newPlan
  }
}

export function updateMockPlanStatus(id, newStatus, note = '') {
  const all = [...getMockStore()]
  const p = all.find(x => x.id === Number(id))
  if (!p) throw new Error('Không tìm thấy kế hoạch')

  p.status = newStatus
  p.lastNote = note || (newStatus === 'APPROVED' ? 'Đã duyệt kế hoạch' : newStatus === 'CANCELLED' ? 'Đã từ chối' : 'Yêu cầu sửa')
  p.histories = p.histories || []
  p.histories.push({
    id: Date.now(),
    action: newStatus,
    actorName: 'Hoàng Thị Loan',
    note: p.lastNote,
    createdAt: new Date().toISOString()
  })
  saveMockStore(all)
  return p
}

export function deleteMockPlan(id) {
  const all = getMockStore().filter(x => x.id !== Number(id))
  saveMockStore(all)
  return true
}

// ==========================================
// MOCK CANDIDATES CHO KẾ HOẠCH ĐỘT XUẤT (UC-12) (+ 5 LỚP MỚI)
// ==========================================
export const MOCK_UNSCHEDULED_CANDIDATES = [
  {
    teachingId: 4,
    classCode: 'IT18303',
    subjectCode: 'WEB1013',
    subjectName: 'Thiết kế Web với HTML5 & CSS3',
    teacherAccount: 'hah7',
    teacherName: 'Hoàng Anh Huy',
    sessionCount: 4,
    plannedDate: '2026-10-21',
    shift: 4,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin'
  },
  {
    teachingId: 5,
    classCode: 'CB18301',
    subjectCode: 'ENG101',
    subjectName: 'Tiếng Anh 1.1',
    teacherAccount: 'huongnt',
    teacherName: 'Nguyễn Thị Hương',
    sessionCount: 2,
    plannedDate: '2026-10-22',
    shift: 1,
    facultyId: 2,
    facultyName: 'Cơ bản'
  },
  // 5 lớp học mới bổ sung:
  {
    teachingId: 3,
    classCode: 'IT18302',
    subjectCode: 'MOB1014',
    subjectName: 'Lập trình Java 1',
    teacherAccount: 'tunglt',
    teacherName: 'Lê Thanh Tùng',
    sessionCount: 4,
    plannedDate: '2026-10-23',
    shift: 2,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin'
  },
  {
    teachingId: 6,
    classCode: 'WD18302',
    subjectCode: 'WEB2014',
    subjectName: 'Xây dựng trang Web với JavaScript',
    teacherAccount: 'minhtv',
    teacherName: 'Trần Văn Minh',
    sessionCount: 4,
    plannedDate: '2026-10-24',
    shift: 3,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin'
  },
  {
    teachingId: 7,
    classCode: 'CS18301',
    subjectCode: 'MAT101',
    subjectName: 'Toán rời rạc',
    teacherAccount: 'nhungpt_hn',
    teacherName: 'Phạm Thu Nhung',
    sessionCount: 3,
    plannedDate: '2026-10-25',
    shift: 1,
    facultyId: 2,
    facultyName: 'Cơ bản'
  },
  {
    teachingId: 8,
    classCode: 'UD18301',
    subjectCode: 'SOF203',
    subjectName: 'Lập trình Java 2',
    teacherAccount: 'haidv',
    teacherName: 'Đỗ Văn Hải',
    sessionCount: 4,
    plannedDate: '2026-10-26',
    shift: 2,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin'
  },
  {
    teachingId: 2,
    classCode: 'CP18301',
    subjectCode: 'PRO2113',
    subjectName: 'Dự án tốt nghiệp (CNTT)',
    teacherAccount: 'linhdt',
    teacherName: 'Đặng Thùy Linh',
    sessionCount: 3,
    plannedDate: '2026-10-27',
    shift: 3,
    facultyId: 1,
    facultyName: 'Công nghệ thông tin'
  }
]

// ==========================================
// QUẢN LÝ IMPORT LỊCH GIẢNG DẠY (MOCK CLIENT-SIDE)
// ==========================================
const JOBS_STORAGE_KEY = 'qldg_mock_jobs_store'
const JOB_ROWS_STORAGE_KEY = 'qldg_mock_job_rows_store'
const NOTIS_STORAGE_KEY = 'qldg_mock_notis_store'

export function getStoredImportJobs(page = 0, size = 10) {
  let list = []
  try {
    const raw = localStorage.getItem(JOBS_STORAGE_KEY)
    list = raw ? JSON.parse(raw) : [...MOCK_JOBS]
  } catch (e) {
    list = [...MOCK_JOBS]
  }
  const from = page * size
  const content = list.slice(from, from + size)
  return {
    content,
    totalElements: list.length,
    totalPages: Math.ceil(list.length / size) || 1,
    number: page
  }
}

export function saveStoredImportJobs(jobs) {
  try {
    localStorage.setItem(JOBS_STORAGE_KEY, JSON.stringify(jobs))
  } catch (e) {
    console.error('Lỗi lưu mock jobs:', e)
  }
}

export function getMockJobDetail(jobId) {
  let list = []
  try {
    const raw = localStorage.getItem(JOBS_STORAGE_KEY)
    list = raw ? JSON.parse(raw) : [...MOCK_JOBS]
  } catch {
    list = [...MOCK_JOBS]
  }
  const found = list.find(j => j.id === Number(jobId))
  return found || list[0] || MOCK_JOBS[0]
}

export function getMockJobRows(jobId, { page = 0, size = 20, result = 'ALL', search = '' } = {}) {
  let rowsMap = {}
  try {
    const raw = localStorage.getItem(JOB_ROWS_STORAGE_KEY)
    if (raw) rowsMap = JSON.parse(raw)
  } catch {}

  let rows = rowsMap[jobId] || MOCK_JOB_ROWS
  if (result && result !== 'ALL') {
    rows = rows.filter(r => r.result === result)
  }
  if (search && search.trim()) {
    const q = search.trim().toLowerCase()
    rows = rows.filter(r =>
      (r.classCode && r.classCode.toLowerCase().includes(q)) ||
      (r.subjectCode && r.subjectCode.toLowerCase().includes(q)) ||
      (r.teacherAccount && r.teacherAccount.toLowerCase().includes(q))
    )
  }

  const from = page * size
  const content = rows.slice(from, from + size)
  return {
    content,
    totalElements: rows.length,
    totalPages: Math.ceil(rows.length / size) || 1,
    currentPage: page
  }
}

export function saveStoredJobRows(jobId, rows) {
  try {
    let rowsMap = {}
    const raw = localStorage.getItem(JOB_ROWS_STORAGE_KEY)
    if (raw) rowsMap = JSON.parse(raw)
    rowsMap[jobId] = rows
    localStorage.setItem(JOB_ROWS_STORAGE_KEY, JSON.stringify(rowsMap))
  } catch (e) {
    console.error('Lỗi lưu mock job rows:', e)
  }
}

export function getMockJobAlerts(jobId) {
  return MOCK_JOB_ALERTS
}

// Xử lý đọc file Excel thực tế bằng SheetJS và lưu vào Mock Store
export async function importScheduleFileMock(file) {
  let totalRows = 0
  let generatedRows = []

  try {
    const buffer = await file.arrayBuffer()
    const workbook = XLSX.read(buffer, { type: 'array' })
    const sheetName = workbook.SheetNames[0]
    const worksheet = workbook.Sheets[sheetName]
    const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 })

    if (jsonData && jsonData.length > 1) {
      totalRows = jsonData.length - 1
      for (let i = 1; i < Math.min(jsonData.length, 50); i++) {
        const row = jsonData[i]
        if (!row || row.length === 0) continue
        generatedRows.push({
          id: i,
          rowNo: i + 1,
          classCode: String(row[0] || 'WD1830' + (i % 5)),
          subjectCode: String(row[1] || 'COM10' + (i % 3)),
          teacherAccount: String(row[3] || 'namnv'),
          sessionNo: Number(row[7]) || (i % 4) + 1,
          result: i % 12 === 0 ? 'SKIPPED' : i % 8 === 0 ? 'UPDATED_ROOM' : 'INSERTED',
          oldValue: i % 8 === 0 ? 'F201' : null,
          newValue: i % 8 === 0 ? 'B203' : `${row[4] || '2026-10-25'} (Ca ${row[5] || 2}, ${row[6] || 'F201'})`,
          message: i % 12 === 0 ? 'Trùng hoàn toàn buổi học đã có' : i % 8 === 0 ? 'Đổi phòng học sang B203' : 'Thêm mới buổi học thành công'
        })
      }
    }
  } catch (e) {
    console.warn('Lỗi đọc nội dung file Excel bằng SheetJS, áp dụng cấu hình ước lượng:', e)
  }

  if (totalRows === 0) totalRows = 145
  const inserted = Math.round(totalRows * 0.85)
  const updated = Math.round(totalRows * 0.10)
  const skipped = Math.max(0, totalRows - inserted - updated)

  let list = []
  try {
    const raw = localStorage.getItem(JOBS_STORAGE_KEY)
    list = raw ? JSON.parse(raw) : [...MOCK_JOBS]
  } catch {
    list = [...MOCK_JOBS]
  }

  const newId = list.length > 0 ? Math.max(...list.map(j => j.id)) + 1 : 1
  const newJob = {
    id: newId,
    fileName: file.name,
    fileSize: file.size,
    status: 'DONE',
    totalRows,
    inserted,
    updated,
    skipped,
    errors: 0,
    progress: 100,
    message: `Thành công: ${inserted}, Cập nhật: ${updated}, Bỏ qua: ${skipped}, Lỗi: 0`,
    createdAt: new Date().toISOString(),
    finishedAt: new Date().toISOString(),
    uploader: { fullName: 'Nguyễn Anh Tuấn', account: 'tuanna' }
  }

  list.unshift(newJob)
  saveStoredImportJobs(list)

  // Lưu rows tương ứng
  saveStoredJobRows(newId, generatedRows.length > 0 ? generatedRows : MOCK_JOB_ROWS)

  // Thêm thông báo chuông
  addMockNotification({
    id: Date.now(),
    message: `File lịch dạy '${file.name}' đã xử lý xong. Hoàn thành ${totalRows} dòng không có lỗi.`,
    link: `/import/${newId}`,
    read: false,
    createdAt: new Date().toISOString()
  })

  return {
    jobId: newId,
    fileName: newJob.fileName,
    status: 'DONE',
    message: `Đã tiếp nhận và xử lý file '${file.name}' thành công (${totalRows} dòng).`
  }
}

// Tạo File Excel mẫu thực tế tải về máy
export function createSampleTemplateExcelBlob() {
  const wb = XLSX.utils.book_new()
  const data = [
    ['Mã lớp', 'Mã môn', 'Tên môn học', 'Tài khoản GV', 'Ngày học', 'Ca học', 'Phòng', 'Buổi số'],
    ['WD18301', 'COM108', 'Nhập môn lập trình', 'namnv', '2026-10-20', 2, 'F201', 1],
    ['WD18301', 'COM108', 'Nhập môn lập trình', 'namnv', '2026-10-22', 2, 'F201', 2],
    ['WD18301', 'COM108', 'Nhập môn lập trình', 'namnv', '2026-10-24', 2, 'F201', 3],
    ['CP18301', 'PRO2113', 'Dự án tốt nghiệp (CNTT)', 'linhdt', '2026-10-21', 3, 'F202', 1],
    ['CP18301', 'PRO2113', 'Dự án tốt nghiệp (CNTT)', 'linhdt', '2026-10-23', 3, 'F202', 2],
    ['IT18302', 'MOB1014', 'Lập trình Java 1', 'tunglt', '2026-10-20', 1, 'L102', 1],
    ['IT18303', 'WEB1013', 'Thiết kế Web với HTML5 & CSS3', 'hah7', '2026-10-21', 4, 'L103', 1],
    ['CB18301', 'ENG101', 'Tiếng Anh 1.1', 'huongnt', '2026-10-20', 1, 'T301', 1]
  ]
  const ws = XLSX.utils.aoa_to_sheet(data)
  XLSX.utils.book_append_sheet(wb, ws, 'LichGiangDay')
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'binary' })
  function s2ab(s) {
    const buf = new ArrayBuffer(s.length)
    const view = new Uint8Array(buf)
    for (let i = 0; i < s.length; i++) view[i] = s.charCodeAt(i) & 0xff
    return buf
  }
  const blob = new Blob([s2ab(wbout)], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  return URL.createObjectURL(blob)
}

// Quản lý Notification mock
export function getMockNotifications() {
  try {
    const raw = localStorage.getItem(NOTIS_STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch {}
  const initial = [
    { id: 1, message: 'Chào mừng bạn đến với hệ thống Quản lý Kế hoạch Dự giờ QLDG', link: '/plans', read: false, createdAt: '2026-10-18T08:00:00' },
    { id: 2, message: 'Kế hoạch KH-CNTT-FA26-B1-002 đã được Trưởng ban Đào tạo phê duyệt', link: '/plans/2', read: true, createdAt: '2026-10-14T08:30:00' }
  ]
  localStorage.setItem(NOTIS_STORAGE_KEY, JSON.stringify(initial))
  return initial
}

export function addMockNotification(noti) {
  try {
    const notis = getMockNotifications()
    notis.unshift(noti)
    localStorage.setItem(NOTIS_STORAGE_KEY, JSON.stringify(notis))
  } catch (e) {
    console.error('Lỗi lưu mock notification:', e)
  }
}
