<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="close">
    <div class="modal-content" style="max-width: 640px;">
      <div class="modal-header">
        <div class="modal-title">Phân công người dự giờ chéo (Bộ môn / Cơ sở)</div>
        <button class="btn-icon" @click="close">✕</button>
      </div>

      <div class="modal-body">
        <div class="banner-info" style="font-size: 12px; margin-bottom: 14px;">
          Danh sách toàn bộ giảng viên trong hệ thống (14 cơ sở). Giảng viên cùng cơ sở được ưu tiên xếp đầu. Có thể gõ để lọc nhanh.
        </div>

        <div class="form-group" style="margin-bottom: 14px;">
          <label class="form-label">Tìm kiếm nhanh</label>
          <input
            v-model="keyword"
            class="form-control"
            placeholder="Tìm theo account, họ tên, bộ môn hoặc cơ sở..."
          />
        </div>

        <!-- Loading -->
        <div v-if="loading" style="text-align: center; padding: 20px 0; font-size: 13px; color: var(--text-muted);">
          Đang tải danh sách giảng viên...
        </div>

        <!-- Trạng thái trống -->
        <div v-else-if="filteredResults.length === 0" class="empty-state" style="padding: 24px 8px;">
          <div class="empty-icon">?</div>
          <div class="empty-title">Không tìm thấy giảng viên phù hợp</div>
          <div style="font-size: 12px; max-width: 380px; margin: 0 auto; color: var(--text-muted);">
            Thử nhập từ khóa khác hoặc xóa ô tìm kiếm để hiển thị toàn bộ danh sách.
          </div>
        </div>

        <!-- Luôn hiển thị toàn bộ danh sách giảng viên -->
        <div v-else style="display: flex; flex-direction: column; gap: 8px; max-height: 360px; overflow-y: auto;">
          <div
            v-for="u in filteredResults"
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
                Định dạng: <strong>{{ u.account }} – {{ u.facultyName || u.facultyCode || 'N/A' }} – {{ u.campusName }}</strong>
              </div>
            </div>
            <button class="btn btn-outline-primary" style="padding: 4px 10px; font-size: 12px;">Chọn</button>
          </div>
        </div>
      </div>

      <div class="modal-footer" style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 12px; color: var(--text-muted);">
          Đang hiển thị: <strong>{{ filteredResults.length }}</strong> giảng viên
        </span>
        <button class="btn btn-secondary" @click="close">Đóng</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { api } from '../services/api.js'

const props = defineProps({
  isOpen: Boolean,
  excludeAccounts: {
    type: Array,
    default: () => []
  }
})
const emit = defineEmits(['close', 'select'])

const keyword = ref('')
const allUsers = ref([])
const loading = ref(false)

watch(() => props.isOpen, async (open) => {
  if (open) {
    keyword.value = ''
    await loadAllUsers()
  }
})

async function loadAllUsers() {
  loading.value = true
  try {
    allUsers.value = await api.searchInspectors('')
  } catch (e) {
    console.error('Lỗi tải giảng viên:', e)
  } finally {
    loading.value = false
  }
}

const filteredResults = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  let list = allUsers.value
  if (props.excludeAccounts && props.excludeAccounts.length > 0) {
    list = list.filter(u => !props.excludeAccounts.includes(u.account))
  }
  if (!q) return list
  return list.filter(u =>
    (u.account && u.account.toLowerCase().includes(q)) ||
    (u.fullName && u.fullName.toLowerCase().includes(q)) ||
    (u.facultyName && u.facultyName.toLowerCase().includes(q)) ||
    (u.facultyCode && u.facultyCode.toLowerCase().includes(q)) ||
    (u.campusName && u.campusName.toLowerCase().includes(q))
  )
})

function selectUser(user) {
  if (props.excludeAccounts && props.excludeAccounts.includes(user.account)) {
    return
  }
  emit('select', user)
  close()
}

function close() {
  emit('close')
}
</script>
