<template>
  <div>
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <h1 class="page-title">Quản lý kế hoạch dự giờ</h1>
      </div>
      <div v-if="canCreatePlan">
        <router-link to="/plans/create" class="btn btn-primary">
          + Lập kế hoạch mới
        </router-link>
      </div>
    </div>

    <!-- Khối bộ lọc (Card) -->
    <div class="card" style="padding: 16px 20px;">
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; align-items: flex-end;">
        <div class="form-group">
          <label class="form-label">Kỳ học</label>
          <select v-model="selectedSemesterId" class="form-control" @change="onFilterChange">
            <option :value="null">-- Chọn kỳ học --</option>
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name || (s.term + ' ' + s.year) }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Block</label>
          <select v-model="selectedBlock" class="form-control" @change="onFilterChange">
            <option :value="0">-- Tất cả block --</option>
            <option :value="1">Block 1</option>
            <option :value="2">Block 2</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Bộ môn</label>
          <select v-model="selectedFacultyId" class="form-control" @change="onFilterChange">
            <option :value="null">-- Chọn bộ môn --</option>
            <option v-for="f in myFaculties" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Trạng thái</label>
          <select v-model="selectedStatus" class="form-control" @change="onFilterChange">
            <option value="ALL">-- Tất cả trạng thái --</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="SUBMITTED">Chờ duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="REJECTED_NEED_EDIT">Yêu cầu sửa</option>
            <option value="CANCELLED">Đã từ chối</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Tìm theo mã KH</label>
          <input v-model="searchCode" class="form-control" placeholder="Mã KH..." @input="currentPage = 1" />
        </div>
      </div>
    </div>

    <!-- Danh sách kế hoạch (Phân trang 4 kế hoạch 1 trang) -->
    <div v-if="loading" class="card" style="text-align: center; padding: 40px; color: var(--text-muted);">
      Đang tải danh sách kế hoạch...
    </div>

    <div v-else-if="allPlans.length === 0" class="empty-state card">
      <div class="empty-icon">📋</div>
      <div class="empty-title">Không tìm thấy kế hoạch dự giờ</div>
      <div style="font-size: 13px;">Chưa có bản kế hoạch nào khớp với bộ lọc đã chọn.</div>
    </div>

    <div v-else class="card" style="padding: 0; overflow: hidden;">
      <div style="padding: 14px 20px; background: #FFF; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between;">
        <h2 class="card-title" style="margin-bottom: 0;">
          Danh sách kế hoạch dự giờ
        </h2>
        <span class="badge badge-gray">{{ allPlans.length }} kế hoạch</span>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>Mã kế hoạch</th>
              <th>Bộ môn</th>
              <th>Người lập</th>
              <th>Số lớp</th>
              <th>Ngày gửi</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in pagedPlans" :key="p.id">
              <td style="font-weight: 600;">
                <router-link :to="`/plans/${p.id}`" style="color: var(--primary); text-decoration: none;">
                  {{ p.code }}
                </router-link>
                <span v-if="p.unscheduled" class="badge badge-red" style="margin-left: 6px;">ĐỘT XUẤT</span>
              </td>
              <td><span class="badge badge-gray">{{ p.facultyName }}</span></td>
              <td>{{ p.creatorName || p.creatorAccount }}</td>
              <td><strong>{{ p.classCount }}</strong> lớp</td>
              <td>{{ formatDate(p.submittedAt || p.createdAt) }}</td>
              <td>
                <span class="badge" :class="statusBadgeClass(p.status)">
                  {{ statusText(p.status) }}
                </span>
                <div v-if="p.status === 'REJECTED_NEED_EDIT' && p.lastNote" style="font-size: 11px; color: var(--danger); margin-top: 2px; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                  Lý do: {{ p.lastNote }}
                </div>
              </td>
              <td>
                <div style="display: flex; align-items: center; gap: 6px;">
                  <!-- Nút Xem (tất cả mọi vai) -->
                  <router-link :to="`/plans/${p.id}`" class="btn btn-secondary" style="padding: 4px 10px; font-size: 12px;">
                    Xem
                  </router-link>

                  <!-- Nút Sửa & Gửi lại (CNBM/TM khi DRAFT hoặc REJECTED_NEED_EDIT) -->
                  <router-link
                    v-if="canEdit(p)"
                    :to="`/plans/create?editId=${p.id}`"
                    class="btn btn-outline-primary"
                    style="padding: 4px 10px; font-size: 12px;"
                  >
                    Sửa
                  </router-link>

                  <!-- Nút Xóa (Nút viền đỏ giống nút Sửa) -->
                  <button
                    class="btn btn-outline-danger"
                    style="padding: 4px 10px; font-size: 12px;"
                    :title="p.status === 'APPROVED' && !currentUser.headOffice ? 'Kế hoạch đã duyệt cần Super Admin đồng ý để xóa' : 'Xóa kế hoạch'"
                    :disabled="p.status === 'APPROVED' && !currentUser.headOffice"
                    @click="confirmDelete(p)"
                  >
                    Xóa
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang 4 kế hoạch 1 trang -->
      <div v-if="totalPages > 1" class="pagination-bar">
        <div class="pagination-info">
          Trang <strong>{{ currentPage }}</strong> / <strong>{{ totalPages }}</strong> (Tổng <strong>{{ allPlans.length }}</strong> kế hoạch)
        </div>
        <div class="pagination-controls">
          <button class="pagination-btn" :disabled="currentPage <= 1" @click="currentPage--">« Trước</button>
          <button
            v-for="p in totalPages"
            :key="p"
            class="pagination-btn"
            :class="{ active: currentPage === p }"
            @click="currentPage = p"
          >
            {{ p }}
          </button>
          <button class="pagination-btn" :disabled="currentPage >= totalPages" @click="currentPage++">Sau »</button>
        </div>
      </div>
    </div>

    <!-- Modal Xác Nhận Xóa Mạnh (Viền đỏ) -->
    <div v-if="deletingPlan" class="modal-overlay" @click.self="deletingPlan = null">
      <div class="modal-content" style="max-width: 480px; border-top: 4px solid var(--danger);">
        <div class="modal-header">
          <div class="modal-title" style="color: var(--danger);">Xác nhận xóa kế hoạch dự giờ</div>
          <button class="btn-icon" @click="deletingPlan = null">✕</button>
        </div>
        <div class="modal-body" style="font-size: 13px;">
          <p style="font-weight: 600; margin-bottom: 8px;">
            Bạn có chắc chắn muốn xóa kế hoạch <code>{{ deletingPlan.code }}</code>?
          </p>
          <div class="banner-warning">
            ⚠️ <strong>Xóa không khôi phục được:</strong> Mọi buổi dự giờ và toàn bộ phiếu đánh giá liên quan sẽ bị mất hoàn toàn!
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="deletingPlan = null">Hủy bỏ</button>
          <button class="btn btn-danger" @click="executeDelete">Xác nhận xóa vĩnh viễn</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'

const currentUser = ref({})
const semesters = ref([])
const myFaculties = ref([])

const selectedSemesterId = ref(null)
const selectedBlock = ref(0)
const selectedFacultyId = ref(null) // Mặc định: -- Chọn bộ môn --
const selectedStatus = ref('ALL')
const searchCode = ref('')

const groupedPlans = ref({})
const loading = ref(false)
const deletingPlan = ref(null)

// Phân trang 4 kế hoạch 1 trang
const pageSize = 4
const currentPage = ref(1)

const canCreatePlan = computed(() => {
  const role = currentUser.value.role
  return role === 'CNBM' || role === 'TM' || role === 'TBDT' || currentUser.value.headOffice
})

onMounted(async () => {
  try {
    currentUser.value = await api.getMe()
    myFaculties.value = currentUser.value.faculties || []
    semesters.value = await api.getSemesters()
    selectedSemesterId.value = null // Mặc định: -- Chọn kỳ học --
    await fetchPlans()
  } catch (e) {
    console.error(e)
  }
})

function onFilterChange() {
  currentPage.value = 1
  fetchPlans()
}

async function fetchPlans() {
  loading.value = true
  try {
    const res = await api.getPlans({
      semesterId: selectedSemesterId.value,
      block: selectedBlock.value,
      facultyId: selectedFacultyId.value,
      status: selectedStatus.value
    })
    groupedPlans.value = res || {}
    currentPage.value = 1
  } catch (e) {
    toast.error('Lỗi tải danh sách kế hoạch: ' + e.message)
  } finally {
    loading.value = false
  }
}

const filteredGroupedPlans = computed(() => {
  if (!searchCode.value.trim()) return groupedPlans.value
  const q = searchCode.value.trim().toLowerCase()
  const out = {}
  for (const [k, list] of Object.entries(groupedPlans.value)) {
    const filtered = list.filter(p => p.code.toLowerCase().includes(q))
    if (filtered.length > 0) out[k] = filtered
  }
  return out
})

const allPlans = computed(() => {
  const list = []
  for (const [facName, pList] of Object.entries(filteredGroupedPlans.value)) {
    pList.forEach(p => {
      list.push({ ...p, facultyName: p.facultyName || facName })
    })
  }
  return list
})

const totalPages = computed(() => Math.ceil(allPlans.value.length / pageSize) || 1)

const pagedPlans = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return allPlans.value.slice(start, start + pageSize)
})

function canEdit(plan) {
  const role = currentUser.value.role
  const canRole = role === 'CNBM' || role === 'TM' || role === 'TBDT' || currentUser.value.headOffice
  return canRole && (plan.status === 'DRAFT' || plan.status === 'REJECTED_NEED_EDIT')
}

function confirmDelete(plan) {
  deletingPlan.value = plan
}

async function executeDelete() {
  if (!deletingPlan.value) return
  const code = deletingPlan.value.code
  try {
    await api.deletePlan(deletingPlan.value.id)
    deletingPlan.value = null
    toast.success(`Đã xóa kế hoạch ${code} thành công!`)
    await fetchPlans()
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

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('vi-VN')
}
</script>
