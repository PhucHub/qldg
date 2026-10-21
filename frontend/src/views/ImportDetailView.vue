<template>
  <div>
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <h1 class="page-title">{{ job.fileName || 'Chi tiết file lịch dạy' }}</h1>
      </div>
      <div>
        <router-link to="/import" class="btn btn-secondary">← Quay về danh sách</router-link>
      </div>
    </div>

    <!-- Tabs con -->
    <div class="tabs-nav">
      <button class="tab-btn" :class="{ active: currentTab === 'overview' }" @click="currentTab = 'overview'">
        Tổng quan
      </button>
      <button class="tab-btn" :class="{ active: currentTab === 'rows' }" @click="currentTab = 'rows'">
        Chi tiết đối soát ({{ job.totalRows || 0 }})
      </button>
      <button class="tab-btn" :class="{ active: currentTab === 'alerts' }" @click="currentTab = 'alerts'">
        Môn học mới ({{ alerts.length }})
      </button>
    </div>

    <!-- TAB 1: TỔNG QUAN -->
    <div v-if="currentTab === 'overview'">
      <div class="banner-info" style="margin-bottom: 20px;">
        ℹ️ Một buổi học được nhận diện theo số thứ tự buổi trong phân công, không theo ngày. Không có số thứ tự thì đối soát theo ngày + ca; không khớp mới là buổi mới.
      </div>

      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 20px;">
        <div class="card" style="padding: 16px;">
          <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted); font-weight: 600;">Tổng dòng xử lý</div>
          <div style="font-size: 28px; font-weight: 700; margin-top: 4px;">{{ job.totalRows }}</div>
        </div>
        <div class="card" style="padding: 16px;">
          <div style="font-size: 11px; text-transform: uppercase; color: var(--success); font-weight: 600;">Thêm mới</div>
          <div style="font-size: 28px; font-weight: 700; color: var(--success); margin-top: 4px;">{{ job.inserted }}</div>
        </div>
        <div class="card" style="padding: 16px;">
          <div style="font-size: 11px; text-transform: uppercase; color: var(--primary); font-weight: 600;">Cập nhật (phòng/ca)</div>
          <div style="font-size: 28px; font-weight: 700; color: var(--primary); margin-top: 4px;">{{ job.updated }}</div>
        </div>
        <div class="card" style="padding: 16px;">
          <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted); font-weight: 600;">Bỏ qua do trùng</div>
          <div style="font-size: 28px; font-weight: 700; color: var(--text-muted); margin-top: 4px;">{{ job.skipped }}</div>
        </div>
        <div class="card" style="padding: 16px;">
          <div style="font-size: 11px; text-transform: uppercase; color: var(--danger); font-weight: 600;">Dòng lỗi</div>
          <div style="font-size: 28px; font-weight: 700; color: var(--danger); margin-top: 4px;">{{ job.errors }}</div>
        </div>
      </div>

      <div class="card">
        <h2 class="card-title" style="margin-bottom: 12px;">Thông tin phiên import</h2>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; font-size: 13px;">
          <div>Người thực hiện: <strong>{{ job.uploader?.fullName }} ({{ job.uploader?.account }})</strong></div>
          <div>Cơ sở: <strong>{{ job.campus?.name }}</strong></div>
          <div>Thời gian tải: <strong>{{ formatDate(job.createdAt) }}</strong></div>
          <div>Hoàn thành lúc: <strong>{{ formatDate(job.finishedAt) }}</strong></div>
        </div>
      </div>
    </div>

    <!-- TAB 2: CHI TIẾT ĐỐI SOÁT -->
    <div v-if="currentTab === 'rows'">
      <div class="card" style="margin-bottom: 16px; padding: 14px 20px;">
        <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;">
          <div style="display: flex; gap: 12px; flex: 1; max-width: 600px;">
            <div class="form-group" style="flex: 1;">
              <label class="form-label">Tìm kiếm lớp / môn / GV</label>
              <input v-model="searchQuery" class="form-control" placeholder="Nhập từ khóa..." @input="fetchRows" />
            </div>
            <div class="form-group" style="width: 200px;">
              <label class="form-label">Loại kết quả đối soát</label>
              <select v-model="selectedResultType" class="form-control" @change="fetchRows">
                <option value="ALL">Tất cả kết quả</option>
                <option value="INSERTED">Thêm mới</option>
                <option value="SKIPPED">Bỏ qua do trùng</option>
                <option value="UPDATED_ROOM">Đổi phòng</option>
                <option value="UPDATED_TIME">Đổi ngày / ca</option>
                <option value="ERROR">Dòng lỗi</option>
              </select>
            </div>
          </div>
          <button v-if="job.errors > 0" class="btn btn-outline-danger" @click="handleExportErrors">
            Tải danh sách lỗi (Excel)
          </button>
        </div>
      </div>

      <div class="card" style="padding: 0; overflow: hidden;">
        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>Dòng</th>
                <th>Mã lớp</th>
                <th>Mã môn</th>
                <th>Tài khoản GV</th>
                <th>STT buổi</th>
                <th>Kết quả đối soát</th>
                <th>Chi tiết thay đổi / Thông báo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="pagedRows.length === 0">
                <td colspan="7" style="text-align: center; padding: 30px; color: var(--text-muted);">
                  Không có dòng dữ liệu nào khớp bộ lọc.
                </td>
              </tr>
              <tr v-for="r in pagedRows" :key="r.id">
                <td>#{{ r.rowNo }}</td>
                <td style="font-weight: 600;">{{ r.classCode }}</td>
                <td>{{ r.subjectCode }}</td>
                <td>{{ r.teacherAccount }}</td>
                <td>{{ r.sessionNo ? 'Buổi ' + r.sessionNo : '-' }}</td>
                <td>
                  <span class="badge" :class="rowResultBadgeClass(r.result)">
                    {{ rowResultText(r.result) }}
                  </span>
                </td>
                <td style="font-size: 12px;">
                  <div v-if="r.result === 'UPDATED_ROOM'">
                    Phòng: <span style="text-decoration: line-through; color: var(--text-muted);">{{ r.oldValue }}</span> → <strong style="color: var(--primary);">{{ r.newValue }}</strong>
                  </div>
                  <div v-else-if="r.result === 'UPDATED_TIME'">
                    Lịch: <span style="text-decoration: line-through; color: var(--text-muted);">{{ r.oldValue }}</span> → <strong style="color: var(--primary);">{{ r.newValue }}</strong>
                  </div>
                  <div v-else-if="r.result === 'ERROR'" style="color: var(--danger); font-weight: 500;">
                    {{ r.message }}
                  </div>
                  <div v-else-if="r.message" style="color: var(--text-muted);">
                    {{ r.message }}
                  </div>
                  <div v-else style="color: var(--text-light);">-</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Phân trang 4 dòng 1 trang -->
        <div v-if="totalRowPages > 1" class="pagination-bar">
          <div class="pagination-info">
            Trang <strong>{{ rowPage }}</strong> / <strong>{{ totalRowPages }}</strong> (Tổng <strong>{{ rows.length }}</strong> dòng)
          </div>
          <div class="pagination-controls">
            <button class="pagination-btn" :disabled="rowPage <= 1" @click="rowPage--">« Trước</button>
            <button
              v-for="p in totalRowPages"
              :key="p"
              class="pagination-btn"
              :class="{ active: rowPage === p }"
              @click="rowPage = p"
            >
              {{ p }}
            </button>
            <button class="pagination-btn" :disabled="rowPage >= totalRowPages" @click="rowPage++">Sau »</button>
          </div>
        </div>
      </div>
    </div>

    <!-- TAB 3: CẢNH BÁO MÔN HỌC MỚI -->
    <div v-if="currentTab === 'alerts'">
      <div class="banner-info" style="margin-bottom: 20px;">
        Môn học mới được tạo tự động khi import. Hãy đặt tên và gán đúng bộ môn để phân loại lớp dự giờ chính xác.
      </div>

      <div v-if="alerts.length === 0" class="empty-state card">
        <div class="empty-icon">✓</div>
        <div class="empty-title">Không phát hiện môn học mới</div>
        <div style="font-size: 13px;">Tất cả mã môn trong file import đều đã tồn tại trong danh mục môn học.</div>
      </div>

      <div v-else style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px;">
        <div v-for="a in alerts" :key="a.id" class="card" style="border-left: 4px solid var(--primary);">
          <div style="display: flex; justify-content: space-between; align-items: flex-start;">
            <div>
              <span class="badge" :class="a.kind === 'UPDATE_VERSION' ? 'badge-blue' : 'badge-red'">
                {{ a.kind === 'UPDATE_VERSION' ? 'Bản cập nhật' : 'Mã mới hoàn toàn' }}
              </span>
              <div style="font-size: 18px; font-weight: 700; margin-top: 6px;">{{ a.subjectCode }}</div>
              <div style="font-size: 13px; color: var(--text-muted);">{{ a.subjectName }}</div>
            </div>
          </div>
          <div style="margin-top: 12px; font-size: 12px; color: var(--text-muted);">
            <div v-if="a.kind === 'UPDATE_VERSION'">
              Bản cập nhật của <strong>{{ a.baseCode }}</strong> – đã tự động chép tên và bộ môn.
            </div>
            <div v-else>
              Mã chưa có trong danh mục – đang để tên tạm và gán bộ môn mặc định.
            </div>
            <div style="margin-top: 4px;">Số lớp bị ảnh hưởng: <strong>{{ a.classCount }} lớp</strong></div>
          </div>
          <div style="margin-top: 14px; text-align: right;">
            <!-- Liên kết sang route giữ chỗ của Môn & Bộ môn (không xây màn này) -->
            <router-link to="/placeholder?target=subjects" class="btn btn-outline-primary" style="font-size: 12px; padding: 4px 12px;">
              Đặt tên môn học →
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as XLSX from 'xlsx'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'

const route = useRoute()
const jobId = route.params.id
const currentTab = ref('overview')

const job = ref({})
const rows = ref([])
const alerts = ref([])
const searchQuery = ref('')
const selectedResultType = ref('ALL')
const exportErrorsUrl = ref('')

// Phân trang 4 dòng 1 trang
const rowPage = ref(1)
const totalRowPages = computed(() => Math.ceil(rows.value.length / 4) || 1)
const pagedRows = computed(() => {
  const start = (rowPage.value - 1) * 4
  return rows.value.slice(start, start + 4)
})

onMounted(async () => {
  exportErrorsUrl.value = await api.getExportErrorsUrl(jobId)
  await loadJob()
  await fetchRows()
  await fetchAlerts()
})

async function handleExportErrors() {
  try {
    const res = await fetch(exportErrorsUrl.value)
    if (res.ok) {
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `Danh_sach_dong_loi_Job${jobId}.xlsx`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
      toast.success('Đã tải danh sách dòng lỗi!')
      return
    }
  } catch (err) {
    console.warn('Backend export không khả dụng, sử dụng Blob fallback:', err)
  }

  // Blob fallback: Lấy dữ liệu dòng lỗi từ api/mock và xuất Excel trực tiếp
  try {
    const errorRes = await api.getJobRows(jobId, { page: 0, size: 500, result: 'ERROR' })
    const errorRows = (errorRes.content || []).filter(r => r.result === 'ERROR')
    
    if (errorRows.length === 0) {
      toast.info('Phiên import này không có dòng lỗi nào cần xuất.')
      return
    }

    const dataToExport = errorRows.map(r => ({
      'Dòng': r.rowNo,
      'Mã lớp': r.classCode || '',
      'Mã môn': r.subjectCode || '',
      'Tài khoản GV': r.teacherAccount || '',
      'Số TT buổi': r.sessionNo != null ? r.sessionNo : '',
      'Lý do lỗi': r.message || 'Lỗi dữ liệu'
    }))

    const worksheet = XLSX.utils.json_to_sheet(dataToExport)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Dong_Loi')
    
    const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `Danh_sach_dong_loi_Job${jobId}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    toast.success('Đã xuất file Excel danh sách lỗi thành công!')
  } catch (e) {
    toast.error('Lỗi khi tải danh sách lỗi: ' + e.message)
  }
}

async function loadJob() {
  try {
    job.value = await api.getJobDetail(jobId)
  } catch (e) {
    toast.error('Lỗi tải thông tin phiên import: ' + e.message)
  }
}

async function fetchRows() {
  try {
    rowPage.value = 1
    const res = await api.getJobRows(jobId, {
      page: 0,
      size: 50,
      result: selectedResultType.value,
      search: searchQuery.value
    })
    rows.value = res.content || []
  } catch (e) {
    toast.error('Lỗi tải chi tiết đối soát: ' + e.message)
  }
}

async function fetchAlerts() {
  try {
    alerts.value = await api.getJobAlerts(jobId)
  } catch (e) {
    console.error(e)
  }
}

function rowResultBadgeClass(res) {
  switch (res) {
    case 'INSERTED': return 'badge-green'
    case 'SKIPPED': return 'badge-gray'
    case 'UPDATED_ROOM':
    case 'UPDATED_TIME':
    case 'SWAPPED': return 'badge-blue'
    case 'ERROR': return 'badge-red'
    default: return 'badge-gray'
  }
}

function rowResultText(res) {
  switch (res) {
    case 'INSERTED': return 'Thêm mới'
    case 'SKIPPED': return 'Bỏ qua do trùng'
    case 'UPDATED_ROOM': return 'Đổi phòng'
    case 'UPDATED_TIME': return 'Đổi ngày/ca'
    case 'SWAPPED': return 'Hoán đổi ngày'
    case 'ERROR': return 'Dòng lỗi'
    default: return res
  }
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString('vi-VN')
}
</script>
