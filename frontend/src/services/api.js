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
        faculties: mock.MOCK_FACULTIES,
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
    return await request('/import/upload', { method: 'POST', body: fd })
  },

  async getImportJobs(page = 0, size = 10) {
    try {
      return await request(`/import/jobs?page=${page}&size=${size}`)
    } catch {
      return {
        content: mock.MOCK_JOBS,
        totalElements: mock.MOCK_JOBS.length,
        totalPages: 1,
        number: page
      }
    }
  },

  async getSampleTemplateUrl() {
    return `${API_BASE}/import/sample-template`
  },

  // UC-08: Đối soát dữ liệu lịch dạy
  async getJobDetail(jobId) {
    try {
      return await request(`/import/jobs/${jobId}`)
    } catch {
      return mock.MOCK_JOBS.find(j => j.id === Number(jobId)) || mock.MOCK_JOBS[0]
    }
  },

  async getJobRows(jobId, { page = 0, size = 20, result = 'ALL', search = '' } = {}) {
    try {
      const q = new URLSearchParams({ page, size, result, search }).toString()
      return await request(`/import/jobs/${jobId}/rows?${q}`)
    } catch {
      return {
        content: mock.MOCK_JOB_ROWS,
        totalElements: mock.MOCK_JOB_ROWS.length,
        totalPages: 1,
        currentPage: page
      }
    }
  },

  async getJobAlerts(jobId) {
    try {
      return await request(`/import/jobs/${jobId}/alerts`)
    } catch {
      return mock.MOCK_JOB_ALERTS
    }
  },

  async getExportErrorsUrl(jobId) {
    return `${API_BASE}/import/jobs/${jobId}/export-errors`
  },

  // UC-09 & UC-10: Phân loại lớp và lập kế hoạch
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
      return {
        quota: { totalTeachers: 12, requiredQuota: 2, plannedTeachers: 1, percent: 50 },
        mandatory: [
          {
            teachingId: 2,
            classCode: 'CP18301',
            subjectCode: 'PRO2113',
            subjectName: 'Dự án tốt nghiệp (CNTT)',
            facultyId: 1,
            facultyName: 'Công nghệ thông tin',
            teacherAccount: 'linhdt',
            teacherName: 'Đặng Thùy Linh',
            sessionCount: 3,
            groupType: 'MANDATORY',
            reasonBadge: 'Lần đầu dạy'
          }
        ],
        others: [
          {
            teachingId: 1,
            classCode: 'WD18301',
            subjectCode: 'COM108',
            subjectName: 'Nhập môn lập trình',
            facultyId: 1,
            facultyName: 'Công nghệ thông tin',
            teacherAccount: 'namnv',
            teacherName: 'Nguyễn Văn Nam',
            sessionCount: 4,
            groupType: 'OTHERS'
          }
        ],
        alreadyPlanned: []
      }
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
      return mock.MOCK_UNSCHEDULED_CANDIDATES
    }
  },

  async getValidSessions(teachingId) {
    try {
      return await request(`/plans/teaching-sessions/${teachingId}`)
    } catch {
      return [
        { sessionId: 1, date: '2026-10-20', shift: 2, room: 'F201', sessionNo: 1, label: '2026-10-20 – Ca 2 (Buổi 1, P.F201)' },
        { sessionId: 2, date: '2026-10-22', shift: 2, room: 'F201', sessionNo: 2, label: '2026-10-22 – Ca 2 (Buổi 2, P.F201)' },
        { sessionId: 3, date: '2026-10-24', shift: 2, room: 'F201', sessionNo: 3, label: '2026-10-24 – Ca 2 (Buổi 3, P.F201)' }
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
        { id: 4, account: 'haidv', fullName: 'Đỗ Văn Hải', facultyName: 'Công nghệ thông tin', campusName: 'CĐ FPT Polytechnic Đà Nẵng', sameCampus: true }
      ]
    }
  },

  // UC-11: Tìm giảng viên toàn hệ thống
  async searchInspectors(keyword) {
    try {
      return await request(`/plans/inspectors/search?keyword=${encodeURIComponent(keyword || '')}`)
    } catch {
      return [
        { id: 6, account: 'namnv', fullName: 'Nguyễn Văn Nam', facultyName: 'CNTT', campusName: 'Đà Nẵng', sameCampus: true },
        { id: 10, account: 'nhungpt_hn', fullName: 'Phạm Thu Nhung', facultyName: 'CB', campusName: 'Hà Nội', sameCampus: false }
      ]
    }
  },

  // UC-10, UC-12: Lưu & Gửi kế hoạch
  async savePlanDraft(data) {
    try {
      return await request('/plans', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...data, submitAction: 'DRAFT' })
      })
    } catch {
      return mock.saveOrSubmitMockPlan(data, false)
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
      return mock.getMockPlansGrouped({ semesterId, block, facultyId, status })
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
      return []
    }
  },

  async markNotificationRead(id) {
    try {
      return await request(`/notifications/${id}/read`, { method: 'POST' })
    } catch {
      return null
    }
  }
}
