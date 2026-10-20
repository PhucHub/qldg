<template>
  <div class="inspection-results-view">
    <!-- Header -->
    <div class="view-header">
      <div>
        <h1 class="view-title">Kết quả dự giờ & Giám sát tiến độ</h1>
        <p class="view-desc">
          Theo dõi toàn bộ các buổi dự giờ, giám sát cảnh báo quá hạn SLA 72 giờ và trích xuất kết quả đánh giá theo phạm vi quản lý (HDSD mục 4.4, SRS UC-15, UC-17).
        </p>
      </div>
      <div class="header-actions">
        <button class="btn btn-outline" @click="fetchData">
          🔄 Làm mới dữ liệu
        </button>
      </div>
    </div>

    <!-- Cảnh báo vi phạm SLA 72h của toàn đơn vị (GBR-05) -->
    <div v-if="overdueCount > 0" class="overdue-banner">
      <div class="overdue-icon">⚠️</div>
      <div class="overdue-content">
        <div class="overdue-title">Cảnh báo vi phạm SLA 72 giờ toàn đơn vị</div>
        <div class="overdue-text">
          Đang có <strong>{{ overdueCount }}</strong> buổi dự giờ đã kết thúc quá hạn 72 giờ nhưng giảng viên dự giờ chưa nộp đủ phiếu! Vui lòng liên hệ và đôn đốc các giảng viên liên quan.
        </div>
      </div>
      <button class="btn btn-danger-banner" @click="filterOverdue">
        Lọc buổi trễ hạn ({{ overdueCount }})
      </button>
    </div>

    <!-- Quick Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-num">{{ totalSessions }}</div>
        <div class="stat-label">Tổng số buổi dự giờ</div>
      </div>
      <div class="stat-card stat-success">
        <div class="stat-num text-success">{{ completedCount }}</div>
        <div class="stat-label">Đã nộp đủ phiếu</div>
      </div>
      <div class="stat-card stat-warning">
        <div class="stat-num text-warning">{{ pendingCount }}</div>
        <div class="stat-label">Đang chờ phiếu</div>
      </div>
      <div class="stat-card stat-danger">
        <div class="stat-num text-danger">{{ overdueCount }}</div>
        <div class="stat-label">Quá hạn SLA 72h</div>
      </div>
    </div>

    <!-- Filter Card -->
    <div class="filter-card">
      <div class="filter-grid">
        <div class="filter-group">
          <label>Học kỳ</label>
          <select v-model="filters.semesterId" @change="fetchData">
            <option value="">Tất cả học kỳ</option>
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Block</label>
          <select v-model="filters.block" @change="fetchData">
            <option value="">Tất cả</option>
            <option value="1">Block 1</option>
            <option value="2">Block 2</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Bộ môn quản lý</label>
          <select
            v-model="filters.facultyId"
            :disabled="allowedFaculties.length === 1"
            @change="fetchData"
          >
            <option v-if="allowedFaculties.length > 1" value="">Tất cả bộ môn</option>
            <option v-for="f in allowedFaculties" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Trạng thái</label>
          <select v-model="filters.statusFilter" @change="fetchData">
            <option value="ALL">Tất cả trạng thái</option>
            <option value="COMPLETED">Đã hoàn thành (đủ phiếu)</option>
            <option value="PENDING">Đang chờ phiếu</option>
            <option value="OVERDUE">⚠️ Quá hạn 72h</option>
          </select>
        </div>

        <div class="filter-group filter-search">
          <label>Tìm kiếm</label>
          <div class="search-input-wrapper">
            <input
              v-model="filters.search"
              type="text"
              placeholder="Giảng viên, mã lớp, môn, kế hoạch..."
              @input="debounceFetch"
            />
            <span v-if="filters.search" class="clear-search" @click="clearSearch">✕</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Sessions Table Card -->
    <div class="card">
      <div v-if="loading" class="empty-state">Đang tải danh sách kết quả dự giờ...</div>
      <div v-else-if="sessions.length === 0" class="empty-state">
        <div class="empty-icon">📊</div>
        <p>Không có buổi dự giờ nào phù hợp với bộ lọc hiện tại.</p>
      </div>
      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th style="width: 170px;">Mã kế hoạch</th>
              <th>Giảng viên được dự</th>
              <th>Môn học & Lớp</th>
              <th style="width: 160px;">Thời gian & Phòng</th>
              <th>Người dự giờ phân công</th>
              <th style="width: 160px; text-align: center;">Kết quả chấm</th>
              <th style="width: 130px; text-align: center;">Trạng thái</th>
              <th style="width: 110px; text-align: center;">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(s, idx) in sessions"
              :key="s.id"
              :class="{ 'row-overdue': s.isOverdue }"
            >
              <td class="text-center font-mono">{{ idx + 1 }}</td>
              <td>
                <router-link :to="`/plans/${s.planId}`" class="plan-link" :title="`Xem chi tiết kế hoạch ${s.planCode}`">
                  {{ s.planCode }}
                </router-link>
              </td>
              <td>
                <div class="teacher-info">
                  <div class="teacher-name">{{ s.teacherName }}</div>
                  <div class="teacher-sub font-mono">({{ s.teacherAccount }}) &bull; {{ s.facultyName }}</div>
                </div>
              </td>
              <td>
                <div class="subject-title">{{ s.subjectName }}</div>
                <div class="subject-meta font-mono">
                  <span class="badge-code">{{ s.subjectCode }}</span>
                  <span class="badge-class">{{ s.classCode }}</span>
                </div>
              </td>
              <td>
                <div class="time-primary">📅 {{ formatDate(s.plannedDate) }}</div>
                <div class="time-sub">Ca {{ s.shift }} &bull; P.{{ s.room || 'F201' }}</div>
              </td>
              <td>
                <div class="inspectors-list">
                  <div
                    v-for="ins in s.assignedInspectors"
                    :key="ins.account"
                    class="inspector-chip"
                    :class="{ submitted: ins.isSubmitted }"
                  >
                    <span class="chip-role">{{ ins.roleLabel }}:</span>
                    <span>{{ ins.fullName }}</span>
                    <span class="chip-status">{{ ins.isSubmitted ? `✓ (${ins.evaluation?.score}đ)` : '⏳ Chưa nộp' }}</span>
                  </div>
                </div>
              </td>
              <td class="text-center">
                <template v-if="s.evaluations && s.evaluations.length > 0">
                  <div class="score-summary">
                    <span class="score-value font-mono">{{ s.avgScore }}/92đ</span>
                    <span class="score-percent">({{ s.avgPercent }}%)</span>
                  </div>
                  <span
                    class="badge-result"
                    :class="s.avgPercent >= 75 ? 'badge-success' : 'badge-danger'"
                  >
                    {{ s.avgPercent >= 75 ? 'ĐẠT (≥ 75%)' : 'CHƯA ĐẠT' }}
                  </span>
                </template>
                <template v-else>
                  <span class="badge-status badge-neutral">
                    Chưa có phiếu
                  </span>
                </template>
              </td>
              <td class="text-center">
                <span v-if="s.isOverdue" class="badge-status badge-danger">
                  ⚠️ Quá hạn 72h
                </span>
                <span v-else-if="s.isCompleted" class="badge-status badge-success">
                  ✓ Hoàn thành
                </span>
                <span v-else class="badge-status badge-warning">
                  ⏳ {{ s.submittedCount }}/{{ s.totalAssigned }} phiếu
                </span>
              </td>
              <td class="text-center">
                <button
                  class="btn-action btn-view"
                  :disabled="!s.evaluations || s.evaluations.length === 0"
                  :title="s.evaluations && s.evaluations.length > 0 ? 'Xem chi tiết phiếu đánh giá' : 'Chưa có phiếu nào được nộp'"
                  @click="openViewModal(s)"
                >
                  👁️ Xem phiếu
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL XEM PHIẾU ĐÁNH GIÁ (UC-17) -->
    <div v-if="showViewModal" class="modal-backdrop" @click.self="closeViewModal">
      <div class="modal-card modal-lg">
        <div class="modal-header">
          <div>
            <h2 class="modal-title">Chi Tiết Phiếu Đánh Giá Dự Giờ (UC-17)</h2>
            <div class="modal-sub">
              Buổi dự: <strong>{{ activeSession?.classCode }} - {{ activeSession?.subjectName }}</strong> &bull;
              GV dạy: <strong>{{ activeSession?.teacherName }} ({{ activeSession?.teacherAccount }})</strong>
            </div>
          </div>
          <button class="modal-close" @click="closeViewModal">&times;</button>
        </div>

        <div class="modal-body printable-area">
          <div v-if="loadingView" class="empty-state">Đang tải phiếu đánh giá...</div>
          <div v-else-if="sessionEvaluations.length === 0" class="empty-state">
            <p>Chưa có phiếu đánh giá nào được nộp cho buổi dự giờ này.</p>
          </div>
          <div v-else>
            <!-- Inspector selector tabs -->
            <div class="eval-tabs">
              <button
                v-for="(ev, index) in sessionEvaluations"
                :key="ev.id"
                class="eval-tab-btn"
                :class="{ active: selectedEvalIndex === index }"
                @click="selectedEvalIndex = index"
              >
                <span>Phiếu {{ index + 1 }}: {{ ev.inspectorName }} ({{ ev.inspectorAccount }})</span>
                <span class="eval-score-pill font-mono">{{ ev.score }}/92đ ({{ ev.percentage }}%)</span>
              </button>
            </div>

            <!-- Current Evaluation Summary -->
            <div v-if="currentSelectedEval" class="eval-detail-sheet">
              <div class="eval-meta-grid">
                <div><strong>Người đánh giá:</strong> {{ currentSelectedEval.inspectorName }} ({{ currentSelectedEval.inspectorAccount }})</div>
                <div><strong>Thời gian gửi phiếu:</strong> {{ formatDateTime(currentSelectedEval.submittedAt) }}</div>
                <div><strong>Tổng điểm:</strong> <span class="font-mono text-bold">{{ currentSelectedEval.score }} / 92 điểm</span></div>
                <div>
                  <strong>Kết quả:</strong>
                  <span
                    class="badge-result"
                    :class="currentSelectedEval.percentage >= 75 ? 'badge-success' : 'badge-danger'"
                  >
                    {{ currentSelectedEval.percentage }}% &bull; {{ currentSelectedEval.percentage >= 75 ? 'ĐẠT YÊU CẦU' : 'KHÔNG ĐẠT' }}
                  </span>
                </div>
              </div>

              <!-- Nhận xét của người dự -->
              <div class="eval-comment-box">
                <div class="comment-heading">📝 Nhận xét & Đánh giá chung:</div>
                <div class="comment-body">{{ currentSelectedEval.note || 'Không có nhận xét thêm.' }}</div>
              </div>

              <!-- Chi tiết điểm 23 câu hỏi -->
              <div class="eval-criteria-table-wrap">
                <table class="data-table print-table">
                  <thead>
                    <tr>
                      <th style="width: 50px;">#</th>
                      <th>Nội dung tiêu chí đánh giá</th>
                      <th style="width: 100px; text-align: center;">Điểm chấm</th>
                      <th style="width: 160px;">Đánh giá</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="c in ALL_CRITERIA" :key="c.id">
                      <td class="text-center font-mono">{{ c.index }}</td>
                      <td>{{ c.text }}</td>
                      <td class="text-center font-mono text-bold">
                        {{ currentSelectedEval.criteriaScores ? currentSelectedEval.criteriaScores[c.id] : 4 }} / 4
                      </td>
                      <td>
                        <span class="score-desc-sm">
                          {{ SCORE_DESCRIPTIONS[currentSelectedEval.criteriaScores ? currentSelectedEval.criteriaScores[c.id] : 4] }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-outline" @click="printEvaluation">
            🖨️ In / Tải PDF
          </button>
          <button class="btn btn-primary" @click="closeViewModal">
            Đóng
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'

const SCORE_DESCRIPTIONS = {
  0: '0 - Chưa thực hiện',
  1: '1 - Yếu / Kém',
  2: '2 - Đạt trung bình',
  3: '3 - Khá / Tốt',
  4: '4 - Xuất sắc'
}

const CRITERIA_GROUPS = [
  {
    id: 'g1',
    num: 'I',
    title: 'Kế hoạch & Chuẩn bị bài giảng',
    items: [
      { id: 'c1', index: 1, text: 'Giáo án, bài giảng chuẩn bị đầy đủ, đúng đề cương chi tiết môn học' },
      { id: 'c2', index: 2, text: 'Chuẩn bị tài liệu học tập, học liệu số và công cụ hỗ trợ giảng dạy' },
      { id: 'c3', index: 3, text: 'Đúng giờ lên lớp, tác phong chuyên nghiệp, trang phục lịch sự' },
      { id: 'c4', index: 4, text: 'Quản lý sĩ số lớp học, điểm danh sinh viên đúng quy định' }
    ]
  },
  {
    id: 'g2',
    num: 'II',
    title: 'Nội dung & Phương pháp giảng dạy',
    items: [
      { id: 'c5', index: 5, text: 'Giới thiệu rõ ràng mục tiêu và nội dung trọng tâm của buổi học' },
      { id: 'c6', index: 6, text: 'Kiến thức truyền đạt chính xác, cập nhật và có tính thực tiễn cao' },
      { id: 'c7', index: 7, text: 'Phương pháp giảng dạy phù hợp (thuyết trình, thảo luận, làm mẫu)' },
      { id: 'c8', index: 8, text: 'Hướng dẫn sinh viên thực hành bài tập/lab cụ thể, rõ ràng' },
      { id: 'c9', index: 9, text: 'Sử dụng ví dụ thực tế phong phú, gắn liền với doanh nghiệp' },
      { id: 'c10', index: 10, text: 'Tạo không khí học tập tích cực, thu hút và lôi cuốn người học' },
      { id: 'c11', index: 11, text: 'Đặt câu hỏi kích thích tư duy và khuyến khích sinh viên phản hồi' },
      { id: 'c12', index: 12, text: 'Theo dõi, quan sát và hỗ trợ từng nhóm sinh viên kịp thời' },
      { id: 'c13', index: 13, text: 'Phân bổ thời gian giữa lý thuyết và thực hành hợp lý' }
    ]
  },
  {
    id: 'g3',
    num: 'III',
    title: 'Kỹ năng sư phạm & Quản lý lớp học',
    items: [
      { id: 'c14', index: 14, text: 'Giọng nói rõ ràng, mạch lạc, truyền cảm, tốc độ phù hợp' },
      { id: 'c15', index: 15, text: 'Sử dụng bảng, slide và công cụ trình chiếu khoa học, thẩm mỹ' },
      { id: 'c16', index: 16, text: 'Kỹ năng lắng nghe, tôn trọng và giải đáp thắc mắc của sinh viên' },
      { id: 'c17', index: 17, text: 'Xử lý linh hoạt và hiệu quả các tình huống phát sinh trong lớp' },
      { id: 'c18', index: 18, text: 'Kiểm soát trật tự và kỷ luật lớp học tốt' }
    ]
  },
  {
    id: 'g4',
    num: 'IV',
    title: 'Đánh giá & Tổng kết buổi học',
    items: [
      { id: 'c19', index: 19, text: 'Kiểm tra mức độ hiểu bài và nắm bắt kiến thức của sinh viên' },
      { id: 'c20', index: 20, text: 'Đánh giá và nhận xét công tâm kết quả thực hành của sinh viên' },
      { id: 'c21', index: 21, text: 'Tổng kết và củng cố các nội dung trọng tâm của buổi học' },
      { id: 'c22', index: 22, text: 'Giao bài tập về nhà và hướng dẫn tự học cho buổi tiếp theo' },
      { id: 'c23', index: 23, text: 'Nhắc nhở tiến độ hoàn thành Assignment / Dự án môn học' }
    ]
  }
]

const ALL_CRITERIA = CRITERIA_GROUPS.flatMap(g => g.items)

// State
const loading = ref(false)
const sessions = ref([])
const totalSessions = ref(0)
const overdueCount = ref(0)
const completedCount = ref(0)
const pendingCount = ref(0)
const allowedFaculties = ref([])

const semesters = ref([
  { id: 1, name: 'Fall 2026' },
  { id: 2, name: 'Summer 2026' },
  { id: 3, name: 'Spring 2026' }
])

const filters = reactive({
  semesterId: '',
  block: '',
  facultyId: '',
  statusFilter: 'ALL',
  search: ''
})

// Modal View state
const showViewModal = ref(false)
const loadingView = ref(false)
const activeSession = ref(null)
const sessionEvaluations = ref([])
const selectedEvalIndex = ref(0)

const currentSelectedEval = computed(() => {
  if (sessionEvaluations.value.length === 0) return null
  return sessionEvaluations.value[selectedEvalIndex.value] || sessionEvaluations.value[0]
})

let debounceTimer = null
function debounceFetch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    fetchData()
  }, 300)
}

function clearSearch() {
  filters.search = ''
  fetchData()
}

function filterOverdue() {
  filters.statusFilter = 'OVERDUE'
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await api.getManagementInspections({
      semesterId: filters.semesterId,
      block: filters.block,
      facultyId: filters.facultyId,
      statusFilter: filters.statusFilter,
      search: filters.search
    })
    sessions.value = res.sessions || []
    totalSessions.value = res.totalSessions || 0
    overdueCount.value = res.overdueCount || 0
    completedCount.value = res.completedCount || 0
    pendingCount.value = res.pendingCount || 0
    allowedFaculties.value = res.allowedFaculties || []

    if (allowedFaculties.value.length === 1 && !filters.facultyId) {
      filters.facultyId = allowedFaculties.value[0].id
    }
  } catch (err) {
    toast.error('Lỗi tải kết quả dự giờ: ' + err.message)
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const parts = dateStr.split('-')
    if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`
    return dateStr
  } catch {
    return dateStr
  }
}

function formatDateTime(dtStr) {
  if (!dtStr) return '-'
  try {
    const d = new Date(dtStr)
    return `${d.toLocaleDateString('vi-VN')} lúc ${d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`
  } catch {
    return dtStr
  }
}

async function openViewModal(session) {
  activeSession.value = session
  selectedEvalIndex.value = 0
  showViewModal.value = true
  loadingView.value = true
  try {
    sessionEvaluations.value = await api.getSessionEvaluations(session.id)
  } catch (err) {
    toast.error('Lỗi tải phiếu đánh giá: ' + err.message)
    sessionEvaluations.value = []
  } finally {
    loadingView.value = false
  }
}

function closeViewModal() {
  showViewModal.value = false
  activeSession.value = null
  sessionEvaluations.value = []
}

function printEvaluation() {
  window.print()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.inspection-results-view {
  max-width: 1240px;
  margin: 0 auto;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.view-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 6px;
}

.view-desc {
  font-size: 13px;
  color: var(--text-muted);
  max-width: 850px;
  line-height: 1.5;
}

/* Stats Cards */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #FFF;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px;
  box-shadow: var(--shadow);
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.text-success { color: var(--success); }
.text-warning { color: #D97706; }
.text-danger { color: var(--danger); }

/* Overdue Banner */
.overdue-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-left: 5px solid #F59E0B;
  border-radius: var(--radius);
  padding: 14px 18px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(245, 158, 11, 0.1);
}

.overdue-icon { font-size: 24px; }
.overdue-content { flex: 1; }
.overdue-title { font-weight: 700; font-size: 14px; color: #B45309; margin-bottom: 2px; }
.overdue-text { font-size: 13px; color: #92400E; }

.btn-danger-banner {
  background: #DC2626;
  color: #FFF;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-danger-banner:hover { background: #B91C1C; }

/* Filter Card */
.filter-card {
  background: #FFF;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow);
}

.filter-grid {
  display: grid;
  grid-template-columns: 160px 120px 220px 190px 1fr;
  gap: 14px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-group label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.filter-group select,
.filter-group input {
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  background: #FFF;
}

.filter-group select:focus,
.filter-group input:focus {
  border-color: var(--primary);
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input-wrapper input {
  width: 100%;
  padding-right: 30px;
}

.clear-search {
  position: absolute;
  right: 10px;
  cursor: pointer;
  color: var(--text-muted);
}

/* Card & Data Table */
.card {
  background: #FFF;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  background: #F9FAFB;
  padding: 12px 14px;
  text-align: left;
  font-weight: 600;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border);
}

.data-table td {
  padding: 14px;
  border-bottom: 1px solid var(--border-light);
  vertical-align: middle;
}

.data-table tr:hover { background: #F9FAFB; }
.row-overdue { background: #FEF2F2 !important; }

.plan-link {
  font-family: monospace;
  font-weight: 600;
  font-size: 12px;
  color: var(--primary);
  text-decoration: none;
  background: var(--primary-bg);
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #BFDBFE;
  display: inline-block;
}

.plan-link:hover {
  text-decoration: underline;
  background: #DBEAFE;
}

.teacher-name { font-weight: 600; color: var(--text-main); }
.teacher-sub { font-size: 12px; color: var(--text-muted); }

.subject-title { font-weight: 600; color: var(--text-main); margin-bottom: 4px; }
.subject-meta { display: flex; gap: 6px; font-size: 11px; }

.badge-code { background: #EFF6FF; color: #1D4ED8; padding: 2px 6px; border-radius: 4px; font-weight: 600; }
.badge-class { background: #F3F4F6; color: #374151; padding: 2px 6px; border-radius: 4px; font-weight: 600; }

.time-primary { font-weight: 600; color: var(--text-main); }
.time-sub { font-size: 12px; color: var(--text-muted); }

.inspectors-list { display: flex; flex-direction: column; gap: 4px; }
.inspector-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 2px 8px;
  background: #F9FAFB;
  border-radius: 4px;
  border: 1px solid var(--border-light);
}

.inspector-chip.submitted {
  background: #F0FDF4;
  border-color: #DCFCE7;
}

.chip-role { font-weight: 700; color: var(--text-muted); font-size: 11px; }
.chip-status { margin-left: auto; font-size: 11px; color: var(--text-muted); }
.inspector-chip.submitted .chip-status { color: #16A34A; font-weight: 600; }

.score-summary { display: flex; align-items: baseline; justify-content: center; gap: 4px; margin-bottom: 2px; }
.score-value { font-size: 14px; font-weight: 700; color: var(--text-main); }
.score-percent { font-size: 11px; color: var(--text-muted); }

.badge-result { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 10px; font-weight: 700; }

.badge-status { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
.badge-danger { background: #FEF2F2; color: #DC2626; border: 1px solid #FECACA; }
.badge-success { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
.badge-warning { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
.badge-neutral { background: #F3F4F6; color: #6B7280; }

.btn-action {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  background: #F3F4F6;
  color: #374151;
  border: 1px solid var(--border);
  transition: all 0.15s;
}

.btn-action:hover:not(:disabled) { background: #E5E7EB; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }

/* Modal Styles */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-card {
  background: #FFF;
  border-radius: var(--radius);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  width: 100%;
}

.modal-lg { max-width: 950px; }

.modal-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #FAFAFA;
}

.modal-title { font-size: 18px; font-weight: 700; color: var(--text-main); }
.modal-sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.modal-close { background: none; border: none; font-size: 24px; line-height: 1; color: var(--text-muted); cursor: pointer; }
.modal-body { padding: 20px 24px; overflow-y: auto; flex: 1; }
.modal-footer { padding: 14px 24px; border-top: 1px solid var(--border); background: #FAFAFA; display: flex; justify-content: flex-end; gap: 12px; }

.eval-tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 1px solid var(--border); padding-bottom: 8px; }
.eval-tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #F3F4F6;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.eval-tab-btn.active {
  background: var(--primary);
  color: #FFF;
  border-color: var(--primary);
}

.eval-score-pill { background: rgba(0, 0, 0, 0.08); padding: 2px 6px; border-radius: 10px; font-size: 11px; }
.eval-tab-btn.active .eval-score-pill { background: rgba(255, 255, 255, 0.25); }

.eval-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 20px;
  background: #F9FAFB;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 14px;
  font-size: 13px;
  margin-bottom: 16px;
}

.eval-comment-box {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 20px;
}

.comment-heading { font-weight: 700; font-size: 13px; color: #92400E; margin-bottom: 4px; }
.comment-body { font-size: 13px; color: #78350F; line-height: 1.5; }

.eval-criteria-table-wrap { border: 1px solid var(--border); border-radius: 6px; overflow: hidden; }
.score-desc-sm { font-size: 12px; color: var(--text-muted); }

.empty-state { text-align: center; padding: 40px 20px; color: var(--text-muted); }
.empty-icon { font-size: 32px; margin-bottom: 8px; }

@media print {
  body * { visibility: hidden; }
  .printable-area, .printable-area * { visibility: visible; }
  .printable-area { position: absolute; left: 0; top: 0; width: 100%; }
}
</style>
