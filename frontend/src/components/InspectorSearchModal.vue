<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="close">
    <div class="modal-content" style="max-width: 600px;">
      <div class="modal-header">
        <div class="modal-title">Phân công người dự giờ chéo (Bộ môn / Cơ sở)</div>
        <button class="btn-icon" @click="close">✕</button>
      </div>

      <div class="modal-body">
        <div class="banner-info" style="font-size: 12px; margin-bottom: 14px;">
          Gõ account hoặc họ tên (từ 2 ký tự). Hệ thống tìm trên toàn bộ 14 cơ sở; giảng viên cùng cơ sở được ưu tiên xếp đầu.
        </div>

        <div class="form-group" style="margin-bottom: 14px;">
          <label class="form-label">Tìm kiếm giảng viên</label>
          <input
            v-model="keyword"
            class="form-control"
            placeholder="Nhập account hoặc tên (vd: hah7, nhungpt)..."
            @input="onSearchInput"
          />
        </div>

        <!-- Trạng thái gợi ý gõ ít hơn 2 ký tự -->
        <div v-if="keyword.trim().length < 2" style="font-size: 12px; color: var(--text-muted); text-align: center; padding: 20px 0;">
          Vui lòng nhập ít nhất 2 ký tự để tìm kiếm.
        </div>

        <!-- Loading -->
        <div v-else-if="loading" style="text-align: center; padding: 20px 0; font-size: 13px; color: var(--text-muted);">
          Đang tìm kiếm giảng viên...
        </div>

        <!-- Trạng thái trống (theo tài liệu SRS) -->
        <div v-else-if="results.length === 0" class="empty-state" style="padding: 24px 8px;">
          <div class="empty-icon">?</div>
          <div class="empty-title">Không tìm thấy giảng viên</div>
          <div style="font-size: 12px; max-width: 380px; margin: 0 auto;">
            Tài khoản có thể chưa tồn tại hoặc đang bị khóa. Hãy liên hệ TC&QLĐT của cơ sở đó để tạo hoặc mở khóa tài khoản.
          </div>
        </div>

        <!-- Danh sách kết quả định dạng: Account - Bộ môn - Cơ sở -->
        <div v-else style="display: flex; flex-direction: column; gap: 8px; max-height: 280px; overflow-y: auto;">
          <div
            v-for="u in results"
            :key="u.id"
            style="padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius); cursor: pointer; display: flex; align-items: center; justify-content: space-between; transition: all 0.15s;"
            :style="{ background: u.sameCampus ? '#FFF' : '#FAFAFA' }"
            @click="selectUser(u)"
          >
            <div>
              <div style="font-weight: 600; font-size: 13px; display: flex; align-items: center; gap: 8px;">
                <span>{{ u.account }}</span>
                <span style="color: var(--text-muted); font-weight: 400;">({{ u.fullName }})</span>
                <span v-if="u.sameCampus" class="badge badge-blue">Cùng cơ sở</span>
                <span v-else class="badge badge-gray">Khác cơ sở</span>
              </div>
              <div style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">
                Định dạng: <strong>{{ u.account }} – {{ u.facultyName || 'N/A' }} – {{ u.campusName }}</strong>
              </div>
            </div>
            <button class="btn btn-outline-primary" style="padding: 4px 10px; font-size: 12px;">Chọn</button>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-secondary" @click="close">Đóng</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { api } from '../services/api.js'

const props = defineProps({
  isOpen: Boolean
})
const emit = defineEmits(['close', 'select'])

const keyword = ref('')
const results = ref([])
const loading = ref(false)
let timer = null

function onSearchInput() {
  clearTimeout(timer)
  if (keyword.value.trim().length < 2) {
    results.value = []
    return
  }
  loading.value = true
  timer = setTimeout(async () => {
    try {
      results.value = await api.searchInspectors(keyword.value.trim())
    } catch (e) {
      console.error(e)
      results.value = []
    } finally {
      loading.value = false
    }
  }, 250)
}

function selectUser(user) {
  emit('select', user)
  close()
}

function close() {
  keyword.value = ''
  results.value = []
  emit('close')
}
</script>
