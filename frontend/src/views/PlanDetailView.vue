<template>
  <div v-if="loading" class="card" style="text-align: center; padding: 40px; color: var(--text-muted);">
    Đang tải chi tiết kế hoạch...
  </div>

  <div v-else>
    <!-- Header trang -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <div style="display: flex; align-items: center; gap: 12px;">
          <h1 class="page-title">{{ plan.code }}</h1>
          <span v-if="plan.unscheduled" class="badge badge-red" style="font-size: 13px;">ĐỘT XUẤT</span>
          <span class="badge" :class="statusBadgeClass(plan.status)" style="font-size: 13px;">
            {{ statusText(plan.status) }}
          </span>
        </div>
      </div>
      <div>
        <router-link to="/plans" class="btn btn-secondary">← Danh sách kế hoạch</router-link>
      </div>
    </div>

    <!-- Banner yêu cầu sửa của TBĐT (hiển thị cho CNBM/TM khi kế hoạch bị REJECTED_NEED_EDIT) -->
    <div v-if="plan.status === 'REJECTED_NEED_EDIT'" class="banner-warning" style="margin-bottom: 20px;">
      <div>
        <div style="font-weight: 700; margin-bottom: 4px;">⚠️ Trưởng ban Đào tạo yêu cầu chỉnh sửa kế hoạch:</div>
        <div>{{ plan.lastNote }}</div>
      </div>
    </div>

    <!-- Banner từ chối -->
    <div v-if="plan.status === 'CANCELLED'" class="card" style="background: #F3F4F6; border-left: 4px solid #4B5563; margin-bottom: 20px; padding: 14px 20px;">
      <div style="font-weight: 700; color: #1F2937;">Kế hoạch đã bị từ chối:</div>
      <div style="font-size: 13px; color: #4B5563; margin-top: 4px;">{{ plan.lastNote }}</div>
    </div>

    <!-- Thông tin chung & Widget chỉ tiêu (Card) -->
    <div class="card" style="margin-bottom: 20px;">
      <h2 class="card-title" style="margin-bottom: 12px;">Thông tin chung</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; font-size: 13px;">
        <div>Kỳ & Block: <strong>{{ plan.semesterName }} - Block {{ plan.block }}</strong></div>
        <div>Bộ môn: <strong>{{ plan.facultyName }}</strong></div>
        <div>Người lập: <strong>{{ plan.creatorName }} ({{ plan.creatorAccount }})</strong></div>
        <div>Ngày gửi: <strong>{{ formatDate(plan.submittedAt || plan.createdAt) }}</strong></div>
      </div>
    </div>

    <!-- BẢNG CHI TIẾT TỪNG BUỔI DỰ GIỜ -->
    <div class="card" style="padding: 0; overflow: hidden; margin-bottom: 24px;">
      <div style="padding: 16px 20px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
        <h2 class="card-title" style="margin-bottom: 0;">Danh sách lớp & người dự giờ ({{ plan.items?.length || 0 }} lớp)</h2>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Lớp</th>
              <th>Môn học</th>
              <th>Giảng viên được dự</th>
              <th>Ngày & Ca dự kiến</th>
              <th>GV 1</th>
              <th>GV 2</th>
              <th>GV 3</th>
              <th v-if="plan.unscheduled">Lý do đột xuất</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(it, idx) in pagedItems" :key="it.id">
              <td>{{ (itemPage - 1) * 4 + idx + 1 }}</td>
              <td style="font-weight: 700; color: var(--primary);">{{ it.classCode }}</td>
              <td>{{ it.subjectCode }} – {{ it.subjectName }}</td>
              <td><strong>{{ it.teacherName }}</strong> ({{ it.teacherAccount }})</td>
              <td>
                <span class="badge badge-blue">
                  {{ formatDateOnly(it.plannedDate) }} – Ca {{ it.shift }}
                </span>
              </td>
              <td>
                <div v-if="it.inspector1">
                  <strong>{{ it.inspector1.account }}</strong>
                  <div style="font-size: 11px; color: var(--text-muted);">{{ it.inspector1.facultyName }} – {{ it.inspector1.campusName }}</div>
                </div>
                <span v-else style="color: var(--danger); font-size: 11px;">Chưa gán</span>
              </td>
              <td>
                <div v-if="it.inspector2">
                  <strong>{{ it.inspector2.account }}</strong>
                  <div style="font-size: 11px; color: var(--text-muted);">{{ it.inspector2.facultyName }} – {{ it.inspector2.campusName }}</div>
                </div>
                <span v-else style="color: var(--danger); font-size: 11px;">Chưa gán</span>
              </td>
              <td>
                <div v-if="it.inspector3">
                  <strong>{{ it.inspector3.account }}</strong>
                  <div style="font-size: 11px; color: var(--text-muted);">{{ it.inspector3.facultyName }} – {{ it.inspector3.campusName }}</div>
                </div>
                <span v-else style="color: var(--text-light); font-size: 12px;">-</span>
              </td>
              <td v-if="plan.unscheduled" style="color: var(--danger); font-size: 12px; font-weight: 500;">
                {{ it.reason }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang 4 lớp 1 trang -->
      <div v-if="totalItemPages > 1" class="pagination-bar">
        <div class="pagination-info">
          Trang <strong>{{ itemPage }}</strong> / <strong>{{ totalItemPages }}</strong> (Tổng <strong>{{ (plan.items || []).length }}</strong> lớp)
        </div>
        <div class="pagination-controls">
          <button class="pagination-btn" :disabled="itemPage <= 1" @click="itemPage--">« Trước</button>
          <button
            v-for="p in totalItemPages"
            :key="p"
            class="pagination-btn"
            :class="{ active: itemPage === p }"
            @click="itemPage = p"
          >
            {{ p }}
          </button>
          <button class="pagination-btn" :disabled="itemPage >= totalItemPages" @click="itemPage++">Sau »</button>
        </div>
      </div>
    </div>

    <!-- DÒNG THỜI GIAN LỊCH SỬ (Timeline) -->
    <div class="card" style="margin-bottom: 80px;">
      <h2 class="card-title" style="margin-bottom: 16px;">Lịch sử phê duyệt</h2>
      <div v-if="!plan.histories || plan.histories.length === 0" style="color: var(--text-muted); font-size: 13px;">
        Chưa có lịch sử thao tác.
      </div>
      <div v-else style="display: flex; flex-direction: column; gap: 16px;">
        <div v-for="h in plan.histories" :key="h.id" style="display: flex; gap: 12px; align-items: flex-start;">
          <div style="width: 10px; height: 10px; border-radius: 50%; background: var(--primary); margin-top: 5px;"></div>
          <div style="font-size: 13px;">
            <div style="font-weight: 600;">
              {{ actionText(h.action) }} bởi <strong>{{ h.actor?.fullName }}</strong> ({{ h.actor?.account }})
              <span style="font-size: 11px; color: var(--text-muted); margin-left: 8px;">{{ formatDate(h.createdAt) }}</span>
            </div>
            <div v-if="h.note" style="color: var(--text-muted); margin-top: 2px;">
              Ghi chú: <em>{{ h.note }}</em>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- THANH HÀNH ĐỘNG CỦA TBĐT (CHỈ HIỆN KHI SUBMITTED & LÀ TBĐT) -->
    <div v-if="canApprove" class="tbdt-action-bar">
      <div style="font-size: 13px; font-weight: 600;">
        Kế hoạch đang chờ bạn duyệt:
      </div>
      <div style="display: flex; gap: 10px;">
        <button class="btn btn-outline-danger" @click="showRejectModal = true">
          ✕ Từ chối
        </button>
        <button class="btn btn-outline-primary" @click="showEditModal = true">
          ✎ Yêu cầu sửa
        </button>
        <button class="btn btn-success" @click="showApproveModal = true">
          ✓ Phê duyệt kế hoạch
        </button>
      </div>
    </div>

    <!-- Nút "Quay về" nổi ở góc dưới (theo tài liệu HDSD mục 3.7) -->
    <router-link to="/plans" class="floating-back-btn" title="Quay về danh sách">
      ← Quay về danh sách
    </router-link>

    <!-- MODAL DUYỆT -->
    <div v-if="showApproveModal" class="modal-overlay" @click.self="showApproveModal = false">
      <div class="modal-content" style="max-width: 440px;">
        <div class="modal-header">
          <div class="modal-title" style="color: var(--success);">Phê duyệt kế hoạch dự giờ</div>
          <button class="btn-icon" @click="showApproveModal = false">✕</button>
        </div>
        <div class="modal-body" style="font-size: 13px;">
          Kế hoạch sẽ chuyển sang trạng thái <strong>ĐÃ DUYỆT (APPROVED)</strong>. Các buổi dự giờ chính thức có hiệu lực và giảng viên sẽ thấy lịch trên trang cá nhân.
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showApproveModal = false">Đóng</button>
          <button class="btn btn-success" @click="executeApprove">Xác nhận duyệt</button>
        </div>
      </div>
    </div>

    <!-- MODAL YÊU CẦU SỬA (Ghi chú bắt buộc) -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-content" style="max-width: 480px;">
        <div class="modal-header">
          <div class="modal-title" style="color: var(--primary);">Yêu cầu CNBM / TM chỉnh sửa kế hoạch</div>
          <button class="btn-icon" @click="showEditModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Ghi chú phản hồi / lý do yêu cầu sửa <span style="color: var(--danger);">*</span></label>
            <textarea
              v-model="editNote"
              class="form-control"
              rows="3"
              placeholder="Ghi rõ nội dung cần sửa (vd: đổi người dự lớp WD18301, bổ sung chỉ tiêu)..."
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showEditModal = false">Đóng</button>
          <button class="btn btn-primary" :disabled="!editNote.trim()" @click="executeRequestEdit">
            Gửi yêu cầu sửa
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL TỪ CHỐI (Lý do bắt buộc) -->
    <div v-if="showRejectModal" class="modal-overlay" @click.self="showRejectModal = false">
      <div class="modal-content" style="max-width: 480px;">
        <div class="modal-header">
          <div class="modal-title" style="color: var(--danger);">Từ chối kế hoạch dự giờ</div>
          <button class="btn-icon" @click="showRejectModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Lý do từ chối <span style="color: var(--danger);">*</span></label>
            <textarea
              v-model="rejectReason"
              class="form-control"
              rows="3"
              placeholder="Nhập lý do từ chối bản kế hoạch này..."
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showRejectModal = false">Đóng</button>
          <button class="btn btn-danger" :disabled="!rejectReason.trim()" @click="executeReject">
            Xác nhận từ chối
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'

const route = useRoute()
const planId = route.params.id

const currentUser = ref({})
const plan = ref({})
const loading = ref(true)

const showApproveModal = ref(false)
const showEditModal = ref(false)
const showRejectModal = ref(false)

const editNote = ref('')
const rejectReason = ref('')

// Phân trang 4 lớp 1 trang
const itemPage = ref(1)
const totalItemPages = computed(() => Math.ceil((plan.value.items || []).length / 4) || 1)
const pagedItems = computed(() => {
  const start = (itemPage.value - 1) * 4
  return (plan.value.items || []).slice(start, start + 4)
})

onMounted(async () => {
  try {
    currentUser.value = await api.getMe()
    await loadPlan()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

async function loadPlan() {
  try {
    plan.value = await api.getPlanDetail(planId)
  } catch (e) {
    toast.error('Lỗi tải kế hoạch: ' + e.message)
  }
}

const canApprove = computed(() => {
  const role = currentUser.value.role
  const isTbdt = role === 'TBDT' || currentUser.value.headOffice
  return isTbdt && plan.value.status === 'SUBMITTED'
})

async function executeApprove() {
  try {
    await api.approvePlan(planId)
    showApproveModal.value = false
    toast.success('Kế hoạch đã được phê duyệt thành công!')
    await loadPlan()
  } catch (e) {
    toast.error('Lỗi: ' + e.message)
  }
}

async function executeRequestEdit() {
  if (!editNote.value.trim()) return
  try {
    await api.requestPlanEdit(planId, editNote.value.trim())
    showEditModal.value = false
    editNote.value = ''
    toast.warning('Đã gửi yêu cầu chỉnh sửa kế hoạch cho người lập!')
    await loadPlan()
  } catch (e) {
    toast.error('Lỗi: ' + e.message)
  }
}

async function executeReject() {
  if (!rejectReason.value.trim()) return
  try {
    await api.rejectPlan(planId, rejectReason.value.trim())
    showRejectModal.value = false
    rejectReason.value = ''
    toast.error('Đã từ chối kế hoạch dự giờ!')
    await loadPlan()
  } catch (e) {
    toast.error('Lỗi: ' + e.message)
  }
}

function statusBadgeClass(s) {
  switch (s) {
    case 'DRAFT': return 'badge-gray'
    case 'SUBMITTED': return 'badge-blue'
    case 'APPROVED': return 'badge-green'
    case 'REJECTED_NEED_EDIT': return 'badge-red'
    case 'CANCELLED': return 'badge-gray'
    default: return 'badge-gray'
  }
}

function statusText(s) {
  switch (s) {
    case 'DRAFT': return 'Bản nháp'
    case 'SUBMITTED': return 'Chờ duyệt'
    case 'APPROVED': return 'Đã duyệt'
    case 'REJECTED_NEED_EDIT': return 'Yêu cầu sửa'
    case 'CANCELLED': return 'Đã từ chối'
    default: return s
  }
}

function actionText(a) {
  switch (a) {
    case 'CREATED': return 'Khởi tạo bản nháp'
    case 'SAVED': return 'Cập nhật bản nháp'
    case 'SUBMITTED': return 'Trình duyệt kế hoạch'
    case 'APPROVED': return 'Phê duyệt kế hoạch'
    case 'REJECTED_NEED_EDIT': return 'Yêu cầu sửa'
    case 'CANCELLED': return 'Từ chối'
    default: return a
  }
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString('vi-VN')
}

function formatDateOnly(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('vi-VN')
}
</script>

<style scoped>
.tbdt-action-bar {
  position: fixed;
  bottom: 0;
  left: 250px;
  right: 0;
  background: #FFF;
  border-top: 2px solid var(--primary);
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4px 12px rgba(0,0,0,0.08);
  z-index: 90;
}

.floating-back-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: #FFF;
  border: 1px solid var(--border);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  padding: 10px 18px;
  border-radius: 30px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  z-index: 80;
  transition: all 0.15s;
}

.floating-back-btn:hover {
  background: var(--gray-bg);
  color: var(--primary);
}
</style>

