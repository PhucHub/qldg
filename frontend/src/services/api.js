import * as mock from '../mock-data/mock.js'

const API_BASE = '/api'

function getAuthHeader() {
  const token = localStorage.getItem('qldg_token') || '1:1:TBDT'
  return { Authorization: `Bearer ${token}` }
}

async function request(url, options = {}) {
  const headers = { ...getAuthHeader(), ...(options.headers || {}) }
  try {
    const res = await fetch(`${API_BASE}${url}`, { ...options, headers })
    if (res.status === 401 || res.status === 403) {
      const err = new Error(res.status === 403 ? 'Không có quyền truy cập' : 'Chưa xác thực')
      err.status = res.status
      throw err
    }
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      throw new Error(data.message || `Lỗi HTTP ${res.status}`)
    }
    const ct = res.headers.get('content-type') || ''
    // Nếu trả về HTML (do Cloudflare SPA fallback), coi như endpoint chưa có và kích hoạt fallback sang mock data
    if (ct.includes('text/html')) {
      throw new Error('API endpoint trả về HTML thay vì JSON (Backend chưa kết nối)')
    }
    if (ct.includes('application/json')) return await res.json()
    return await res.blob()
  } catch (err) {
    console.warn(`API [${url}] fallback to mock:`, err.message)
    throw err
  }
}

export const api = {
  // Auth & Dev Token
  async getMe() {
    try {
      return await request('/auth/me')
    } catch {
      const token = localStorage.getItem('qldg_token') || '1:1:TBDT'
      const parts = token.split(':')
      const userId = Number(parts[0]) || 1
      const u = mock.MOCK_USERS.find(x => x.id === userId) || mock.MOCK_USERS[0]
      return {
        id: u.id,
        account: u.account,
        fullName: u.fullName,
        role: u.role,
        campus: { id: 1, code: u.campusCode, name: u.campusName },
        faculty: u.facultyCode ? { code: u.facultyCode, name: u.facultyCode } : null,
        faculties: mock.getMyFaculties(u),
        headOffice: !!u.headOffice
      }
    }
  },

  async getDevUsers() {
    try {
      return await request('/auth/dev/users')
    } catch {
      return mock.MOCK_USERS
    }
  },

  async createDevToken(userId) {
    try {
      return await request('/auth/dev/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId })
      })
    } catch {
      const u = mock.MOCK_USERS.find(x => x.id === userId) || mock.MOCK_USERS[0]
      return { token: `${u.id}:1:${u.role}` }
    }
  },

  // UC-07: Import lịch giảng dạy
  async uploadScheduleFile(file) {
    const fd = new FormData()
    fd.append('file', file)
    try {
      return await request('/import/upload', { method: 'POST', body: fd })
    } catch (err) {
      console.warn('Backend chưa kết nối, tự động xử lý file trong Chế độ Dữ liệu mẫu (Mock Demo):', err.message)
      return await mock.importScheduleFileMock(file)
    }
  },

  async getImportJobs(page = 0, size = 10) {
    try {
      return await request(`/import/jobs?page=${page}&size=${size}`)
    } catch {
      return mock.getStoredImportJobs(page, size)
    }
  },

  // UC-7-3: Khôi phục danh sách về 2 bản ghi mặc định ban đầu
  async resetDefaultJobs() {
    try {
      return await request('/import/jobs/reset', { method: 'POST' })
    } catch {
      return mock.resetDefaultJobsMock()
    }
  },

  async getSampleTemplateUrl() {
    try {
      const res = await fetch(`${API_BASE}/import/sample-template`, { method: 'HEAD' })
      if (res.ok && !res.headers.get('content-type')?.includes('text/html')) {
        return `${API_BASE}/import/sample-template`
      }
    } catch {}
    return mock.createSampleTemplateExcelBlob()
  },

  // UC-08: Đối soát dữ liệu lịch dạy
  async getJobDetail(jobId) {
    try {
      return await request(`/import/jobs/${jobId}`)
    } catch {
      return mock.getMockJobDetail(jobId)
    }
  },

  async getJobRows(jobId, { page = 0, size = 20, result = 'ALL', search = '' } = {}) {
    try {
      const q = new URLSearchParams({ page, size, result, search }).toString()
      return await request(`/import/jobs/${jobId}/rows?${q}`)
    } catch {
      return mock.getMockJobRows(jobId, { page, size, result, search })
    }
  },

  async getJobAlerts(jobId) {
    try {
      return await request(`/import/jobs/${jobId}/alerts`)
    } catch {
      return mock.getMockJobAlerts(jobId)
    }
  },

  async getExportErrorsUrl(jobId) {
    return `${API_BASE}/import/jobs/${jobId}/export-errors`
  },

  // UC-09 & UC-10: Phân loại lớp và lập kế hoạch (+ 5 LỚP HỌC MỚI BỔ SUNG)
  async getSemesters() {
    try {
      return await request('/plans/semesters')
    } catch {
      return mock.MOCK_SEMESTERS
    }
  },

  async getClassGroups(semesterId, block, facultyId) {
    const q = new URLSearchParams()
    if (semesterId) q.append('semesterId', semesterId)
    if (block !== undefined && block !== null && block !== '') q.append('block', block)
    if (facultyId) q.append('facultyId', facultyId)
    try {
      return await request(`/plans/classify?${q.toString()}`)
    } catch {
      return mock.getMockClassGroups(semesterId, block, facultyId)
    }
  },

  // UC-12: Kế hoạch đột xuất
  async getUnscheduledCandidates(semesterId, block, facultyId) {
    const q = new URLSearchParams()
    if (semesterId) q.append('semesterId', semesterId)
    if (block !== undefined && block !== null && block !== '') q.append('block', block)
    if (facultyId) q.append('facultyId', facultyId)
    try {
      return await request(`/plans/unscheduled-candidates?${q.toString()}`)
    } catch {
      return mock.getMockUnscheduledCandidates(semesterId, block, facultyId)
    }
  },

  async getValidSessions(teachingId) {
    try {
      return await request(`/plans/teaching-sessions/${teachingId}`)
    } catch {
      return [
        { sessionId: 1, date: '2026-10-20', shift: 2, room: 'F201', sessionNo: 1, label: '2026-10-20 – Ca 2 (Buổi 1, P.F201)' },
        { sessionId: 2, date: '2026-10-22', shift: 2, room: 'F201', sessionNo: 2, label: '2026-10-22 – Ca 2 (Buổi 2, P.F201)' },
        { sessionId: 3, date: '2026-10-24', shift: 2, room: 'F201', sessionNo: 3, label: '2026-10-24 – Ca 2 (Buổi 3, P.F201)' },
        { sessionId: 4, date: '2026-10-26', shift: 3, room: 'F202', sessionNo: 4, label: '2026-10-26 – Ca 3 (Buổi 4, P.F202)' },
        { sessionId: 5, date: '2026-10-28', shift: 1, room: 'L102', sessionNo: 5, label: '2026-10-28 – Ca 1 (Buổi 5, P.L102)' }
      ]
    }
  },

  async getInspectorsInFaculty(facultyId) {
    try {
      const q = facultyId ? `?facultyId=${facultyId}` : ''
      return await request(`/plans/inspectors/in-faculty${q}`)
    } catch {
      return [
        { id: 6, account: 'namnv', fullName: 'Nguyễn Văn Nam', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true },
        { id: 4, account: 'haidv', fullName: 'Đỗ Văn Hải', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true },
        { id: 3, account: 'dungpt', fullName: 'Phan Tiến Dũng', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true },
        { id: 8, account: 'linhdt', fullName: 'Đặng Thùy Linh', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true },
        { id: 9, account: 'tunglt', fullName: 'Lê Thanh Tùng', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true },
        { id: 10, account: 'hah7', fullName: 'Hoàng Anh Huy', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true },
        { id: 11, account: 'huongnt', fullName: 'Nguyễn Thị Hương', facultyName: 'Cơ bản', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true },
        { id: 12, account: 'minhtv', fullName: 'Trần Văn Minh', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic TP.HCM', sameCampus: false },
        { id: 13, account: 'nhungpt_hn', fullName: 'Phạm Thu Nhung', facultyName: 'Cơ bản', campusName: 'CĐ FPT Polytechnic Hà Nội', sameCampus: false }
      ]
    }
  },

  // UC-11: Tìm giảng viên toàn hệ thống
  async searchInspectors(keyword) {
    try {
      return await request(`/plans/inspectors/search?keyword=${encodeURIComponent(keyword || '')}`)
    } catch {
      const all = [
        { id: 6, account: 'namnv', fullName: 'Nguyễn Văn Nam', facultyName: 'CNTT', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 4, account: 'haidv', fullName: 'Đỗ Văn Hải', facultyName: 'CNTT', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 3, account: 'dungpt', fullName: 'Phan Tiến Dũng', facultyName: 'CNTT', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 8, account: 'linhdt', fullName: 'Đặng Thùy Linh', facultyName: 'CNTT', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 9, account: 'tunglt', fullName: 'Lê Thanh Tùng', facultyName: 'CNTT', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 10, account: 'hah7', fullName: 'Hoàng Anh Huy', facultyName: 'CNTT', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 11, account: 'huongnt', fullName: 'Nguyễn Thị Hương', facultyName: 'CB', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 1, account: 'loanth', fullName: 'Hoàng Thị Loan', facultyName: 'Ban Đào tạo', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 5, account: 'nhungpt4', fullName: 'Phạm Thị Nhung', facultyName: 'CB', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 12, account: 'minhtv', fullName: 'Trần Văn Minh', facultyName: 'CNTT', campusName: 'TP.HCM', sameCampus: false },
        { id: 13, account: 'nhungpt_hn', fullName: 'Phạm Thu Nhung', facultyName: 'CB', campusName: 'Hà Nội', sameCampus: false }
      ]
      if (!keyword || !keyword.trim()) return all
      const q = keyword.trim().toLowerCase()
      return all.filter(u =>
        u.account.toLowerCase().includes(q) ||
        u.fullName.toLowerCase().includes(q) ||
        u.facultyName.toLowerCase().includes(q) ||
        u.campusName.toLowerCase().includes(q)
      )
    }
  },

  // UC-10, UC-12: Lưu & Gửi kế hoạch
  async savePlanDraft(data, planId = null) {
    const url = planId ? `/plans/${planId}` : '/plans'
    const method = planId ? 'PUT' : 'POST'
    try {
      return await request(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...data, submitAction: 'DRAFT' })
      })
    } catch {
      return mock.saveOrSubmitMockPlan(data, false, planId)
    }
  },

  async submitPlan(data, planId = null) {
    const url = planId ? `/plans/${planId}` : '/plans'
    const method = planId ? 'PUT' : 'POST'
    try {
      return await request(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...data, submitAction: 'SUBMIT' })
      })
    } catch {
      return mock.saveOrSubmitMockPlan(data, true, planId)
    }
  },

  async checkDuplicatePlanCode(code, excludeId = null) {
    try {
      const res = await request(`/plans/check-code?code=${encodeURIComponent(code)}&excludeId=${excludeId || ''}`)
      return res.isDuplicate
    } catch {
      return mock.checkDuplicatePlanCode(code, excludeId)
    }
  },

  // UC-13: Luồng trình duyệt kế hoạch
  async getPlans({ semesterId, block, facultyId = null, status = 'ALL' } = {}) {
    const q = new URLSearchParams()
    if (semesterId) q.append('semesterId', semesterId)
    if (block !== undefined && block !== null && block !== '') q.append('block', block)
    if (status) q.append('status', status)
    if (facultyId) q.append('facultyId', facultyId)
    try {
      return await request(`/plans?${q.toString()}`)
    } catch {
      const me = await this.getMe()
      return mock.getMockPlansGrouped({ semesterId, block, facultyId, status, user: me })
    }
  },

  async getPlanDetail(id) {
    try {
      return await request(`/plans/${id}`)
    } catch {
      return mock.getMockPlanDetail(id)
    }
  },

  async approvePlan(id) {
    try {
      return await request(`/plans/${id}/approve`, { method: 'POST' })
    } catch {
      return mock.updateMockPlanStatus(id, 'APPROVED', 'Đã phê duyệt kế hoạch')
    }
  },

  async requestPlanEdit(id, note) {
    try {
      return await request(`/plans/${id}/request-edit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ note })
      })
    } catch {
      return mock.updateMockPlanStatus(id, 'REJECTED_NEED_EDIT', note)
    }
  },

  async rejectPlan(id, reason) {
    try {
      return await request(`/plans/${id}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason })
      })
    } catch {
      return mock.updateMockPlanStatus(id, 'CANCELLED', reason)
    }
  },

  async deletePlan(id) {
    try {
      return await request(`/plans/${id}`, { method: 'DELETE' })
    } catch {
      return mock.deleteMockPlan(id)
    }
  },

  async getNotifications() {
    try {
      return await request('/notifications')
    } catch {
      return mock.getMockNotifications()
    }
  },

  async markNotificationRead(id) {
    try {
      return await request(`/notifications/${id}/read`, { method: 'POST' })
    } catch {
      return null
    }
  },

  // UC-15: Lịch dự giờ của tôi & cảnh báo quá hạn 72h
  async getMyInspections(params = {}) {
    try {
      const q = new URLSearchParams()
      if (params.semesterId) q.append('semesterId', params.semesterId)
      if (params.block) q.append('block', params.block)
      if (params.statusFilter) q.append('statusFilter', params.statusFilter)
      if (params.search) q.append('search', params.search)
      return await request(`/inspections/my?${q.toString()}`)
    } catch {
      const token = localStorage.getItem('qldg_token') || '1:1:TBDT'
      const parts = token.split(':')
      const userId = parseInt(parts[0], 10) || 1
      const currentUser = mock.MOCK_USERS.find(u => u.id === userId) || { account: 'namnv' }
      return mock.getMyInspectionsMock(currentUser.account, params)
    }
  },

  // UC-16: Nộp phiếu đánh giá
  async submitEvaluation(payload) {
    try {
      return await request('/inspections/evaluations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })
    } catch {
      return mock.submitEvaluationMock(payload)
    }
  },

  // UC-17: Xem các phiếu đánh giá của buổi học
  async getSessionEvaluations(sessionId) {
    try {
      return await request(`/inspections/sessions/${sessionId}/evaluations`)
    } catch {
      return mock.getSessionEvaluationsMock(sessionId)
    }
  },

  // UC-15 & UC-17: Kết quả dự giờ toàn bộ môn / cơ sở cho Cán bộ Quản lý (CNBM, TM, TBĐT, TCQLDT, HeadOffice)
  async getManagementInspections(params = {}) {
    try {
      const q = new URLSearchParams()
      if (params.semesterId) q.append('semesterId', params.semesterId)
      if (params.block) q.append('block', params.block)
      if (params.facultyId) q.append('facultyId', params.facultyId)
      if (params.statusFilter) q.append('statusFilter', params.statusFilter)
      if (params.search) q.append('search', params.search)
      return await request(`/inspections/management?${q.toString()}`)
    } catch {
      const token = localStorage.getItem('qldg_token') || '1:1:TBDT'
      const parts = token.split(':')
      const userId = parseInt(parts[0], 10) || 1
      return mock.getManagementInspectionsMock(userId, params)
    }
  }
}
