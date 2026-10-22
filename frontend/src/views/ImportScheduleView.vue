<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Import lịch giảng dạy</h1>
    </div>

    <!-- Banner kiểm tra mốc thời gian kỳ/block -->
    <div v-if="!semesterReady" class="banner-warning">
      <span>⚠️ Chưa cấu hình mốc thời gian kỳ & block. Vui lòng cập nhật kế hoạch kỳ trước khi import.</span>
    </div>

    <!-- Khu vực Tải lên (Card) -->
    <div class="card">
      <div class="card-title-row">
        <h2 class="card-title">Tải lên file lịch dạy</h2>
        <a :href="sampleUrl" class="btn btn-secondary" download="mau_import_lich_giang_day.xlsx">
          Tải file mẫu (.xlsx)
        </a>
      </div>

      <!-- Chọn Kỳ học & Block (README mục 2.1 Bước 2 & Test Case 2.3) -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 260px)); gap: 14px; margin-bottom: 16px;">
        <div class="form-group" style="margin-bottom: 0;">
          <label class="form-label">Kỳ học</label>
          <select v-model="selectedSemesterId" class="form-control">
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name || (s.term + ' ' + s.year) }}</option>
          </select>
        </div>
        <div class="form-group" style="margin-bottom: 0;">
          <label class="form-label">Block</label>
          <select v-model="selectedBlock" class="form-control">
            <option :value="1">Block 1</option>
            <option :value="2">Block 2</option>
          </select>
        </div>
      </div>

      <div
        class="upload-dropzone"
        :class="{ dragging: isDragging, disabled: !semesterReady }"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="onDrop"
        @click="triggerFileInput"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept=".xlsx, .xls"
          style="display: none"
          @change="onFileChange"
        />
        <div style="font-size: 36px; color: var(--primary); margin-bottom: 8px;">📄</div>
        <div v-if="!selectedFile" style="font-size: 14px; font-weight: 500;">
          Kéo thả file Excel vào đây, hoặc <span style="color: var(--primary); text-decoration: underline;">chọn file từ máy tính</span>
        </div>
        <div v-else style="font-size: 14px; font-weight: 600; color: var(--text-main);">
          {{ selectedFile.name }} ({{ formatSize(selectedFile.size) }})
        </div>
        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
          Chỉ chấp nhận file .xlsx hoặc .xls. Dung lượng tối đa 20MB.
        </div>
      </div>

      <div v-if="errorMsg" class="banner-warning" style="margin-top: 14px; margin-bottom: 0;">
        {{ errorMsg }}
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 16px;">
        <div style="font-size: 12px; color: var(--text-muted);">
          Ghi chú: File lịch cần có cột số thứ tự buổi học. Hãy import từ file, đừng sửa tay từng buổi.
        </div>
        <button
          class="btn btn-primary"
          :disabled="!selectedFile || uploading || !semesterReady"
          @click="uploadFile"
        >
          <span v-if="uploading">Đang tải lên...</span>
          <span v-else>Tải lên & Xử lý nền</span>
        </button>
      </div>
    </div>

    <!-- Banner thông báo tiếp nhận thành công vào hàng đợi -->
    <div v-if="successMsg" class="banner-info" style="margin-bottom: 20px;">
      <span>ℹ️ {{ successMsg }}</span>
    </div>

    <!-- Khu vực Lịch sử Import (Card) -->
    <div class="card">
      <div class="card-title-row">
        <h2 class="card-title">Lịch sử import lịch dạy</h2>
        <div style="display: flex; gap: 8px;">
          <button class="btn btn-secondary" @click="refreshJobs">
            Làm mới
          </button>
          <button class="btn btn-outline" style="font-size: 12px;" title="Khôi phục danh sách về 2 bản ghi mặc định ban đầu (UC-7-3)" @click="resetDefaultJobs">
            Đặt lại 2 bản ghi mặc định
          </button>
        </div>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Tên file</th>
              <th>Người tải</th>
              <th>Thời gian</th>
              <th>Trạng thái</th>
              <th>Tổng dòng</th>
              <th>Thành công</th>
              <th>Bỏ qua</th>
              <th>Cập nhật</th>
              <th>Lỗi</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loadingJobs">
              <td colspan="11" style="text-align: center; padding: 30px; color: var(--text-muted);">
                Đang tải dữ liệu...
              </td>
            </tr>
            <tr v-else-if="jobs.length === 0">
              <td colspan="11" style="text-align: center; padding: 30px; color: var(--text-muted);">
                Chưa có lịch sử import nào.
              </td>
            </tr>
            <tr v-for="(job, index) in jobs" :key="job.id">
              <td>{{ index + 1 }}</td>
              <td style="font-weight: 500;">{{ job.fileName }}</td>
              <td>{{ job.uploader?.fullName || job.uploader?.account || 'N/A' }}</td>
              <td>{{ formatDate(job.createdAt) }}</td>
              <td>
                <span class="badge" :class="statusBadgeClass(job.status)">
                  {{ statusText(job.status) }}
                </span>
                <div v-if="job.status === 'PROCESSING'" style="width: 80px; height: 4px; background: #E5E7EB; border-radius: 2px; margin-top: 4px; overflow: hidden;">
                  <div :style="{ width: job.progress + '%', height: '100%', background: 'var(--primary)' }"></div>
                </div>
              </td>
              <td><strong>{{ job.totalRows }}</strong></td>
              <td style="color: var(--success); font-weight: 600;">{{ job.inserted }}</td>
              <td style="color: var(--text-muted);">{{ job.skipped }}</td>
              <td style="color: var(--primary);">{{ job.updated }}</td>
              <td :style="{ color: job.errors > 0 ? 'var(--danger)' : 'var(--text-muted)', fontWeight: job.errors > 0 ? '700' : 'normal' }">
                {{ job.errors }}
              </td>
              <td>
                <router-link :to="`/import/${job.id}`" class="btn btn-outline-primary" style="padding: 4px 10px; font-size: 12px;">
                  Xem đối soát
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'

const semesters = ref([])
const selectedSemesterId = ref(null)
const selectedBlock = ref(1)
const semesterReady = ref(true)
const selectedFile = ref(null)
const fileInputRef = ref(null)
const isDragging = ref(false)
const uploading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const sampleUrl = ref('')

const jobs = ref([])
const loadingJobs = ref(false)
let pollTimer = null

onMounted(async () => {
  try {
    const semList = await api.getSemesters()
    semesters.value = semList || []
    semesterReady.value = semesters.value.length > 0
    if (semesters.value.length > 0) {
      selectedSemesterId.value = semesters.value[0].id
    }
  } catch {
    semesterReady.value = true
  }
  sampleUrl.value = await api.getSampleTemplateUrl()
  await fetchJobs()
  // Polling mỗi 4 giây để cập nhật tiến trình import
  pollTimer = setInterval(fetchJobs, 4000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

function triggerFileInput() {
  if (!semesterReady.value) return
  fileInputRef.value.click()
}

function onFileChange(e) {
  const file = e.target.files[0]
  validateAndSetFile(file)
}

function onDrop(e) {
  isDragging.value = false
  if (!semesterReady.value) return
  const file = e.dataTransfer.files[0]
  validateAndSetFile(file)
}

function validateAndSetFile(file) {
  errorMsg.value = ''
  successMsg.value = ''
  if (!file) return
  const name = file.name.toLowerCase()
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    errorMsg.value = 'File không đúng định dạng. Chỉ chấp nhận file Excel (.xlsx hoặc .xls).'
    selectedFile.value = null
    return
  }
  selectedFile.value = file
}

async function uploadFile() {
  if (!selectedFile.value) return
  uploading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const res = await api.uploadScheduleFile(selectedFile.value)
    successMsg.value = res.message || 'Đã tiếp nhận file vào hàng đợi xử lý nền. Bạn có thể đóng trình duyệt.'
    toast.success('Import file lịch dạy thành công!')
    selectedFile.value = null
    if (fileInputRef.value) fileInputRef.value.value = ''
    await fetchJobs()
  } catch (err) {
    errorMsg.value = err.message || 'Tải file thất bại'
    toast.error('Lỗi: ' + errorMsg.value)
  } finally {
    uploading.value = false
  }
}

async function fetchJobs() {
  try {
    const res = await api.getImportJobs(0, 15)
    jobs.value = res.content || res
  } catch (err) {
    console.error('Lỗi tải danh sách import:', err)
  }
}

// UC-7-3: Làm mới danh sách và hiển thị thông báo
async function refreshJobs() {
  loadingJobs.value = true
  await fetchJobs()
  loadingJobs.value = false
  toast.success('Đã làm mới danh sách import lịch dạy')
}

// UC-7-3: Reload lại bảng và khôi phục 2 bản ghi mặc định ban đầu
async function resetDefaultJobs() {
  loadingJobs.value = true
  await api.resetDefaultJobs()
  await fetchJobs()
  loadingJobs.value = false
  toast.success('Đã reload lại bảng, khôi phục 2 bản ghi mặc định ban đầu (UC-7-3)')
}

function statusBadgeClass(status) {
  switch (status) {
    case 'PENDING': return 'badge-gray'
    case 'PROCESSING': return 'badge-blue'
    case 'DONE': return 'badge-green'
    case 'DONE_WITH_ERRORS': return 'badge-red'
    case 'FAILED': return 'badge-red'
    default: return 'badge-gray'
  }
}

function statusText(status) {
  switch (status) {
    case 'PENDING': return 'Chờ hàng đợi'
    case 'PROCESSING': return 'Đang xử lý'
    case 'DONE': return 'Hoàn thành'
    case 'DONE_WITH_ERRORS': return 'Hoàn thành có lỗi'
    case 'FAILED': return 'Thất bại'
    default: return status
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('vi-VN')
}
</script>

<style scoped>
.upload-dropzone {
  border: 2px dashed var(--border);
  border-radius: var(--radius);
  padding: 36px 20px;
  text-align: center;
  background-color: #FAFAFA;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-dropzone:hover {
  border-color: var(--primary);
  background-color: var(--primary-bg);
}

.upload-dropzone.dragging {
  border-color: var(--primary);
  background-color: var(--primary-bg);
}

.upload-dropzone.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}
</style>
