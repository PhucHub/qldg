<template>
  <div class="card empty-state" style="max-width: 600px; margin: 40px auto; padding: 40px 20px;">
    <div class="empty-icon">{{ isForbidden ? '⛔' : '🚧' }}</div>
    <div class="empty-title">{{ isForbidden ? 'Không có quyền truy cập' : 'Chức năng ngoài phạm vi đảm nhận' }}</div>
    <p style="font-size: 13px; color: var(--text-muted); margin: 8px 0 20px 0;">
      <template v-if="isForbidden">
        {{ roleErrorMessage }}
      </template>
      <template v-else>
        Trang "{{ pageName }}" do nhóm khác phụ trách (quản trị hệ thống, dữ liệu nền hoặc phiếu đánh giá/báo cáo).
      </template>
    </p>
    <router-link to="/plans" class="btn btn-primary">
      Quay về Kế hoạch dự giờ
    </router-link>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isForbidden = computed(() => route.query.target === 'unauthorized')

const userRole = computed(() => {
  const token = localStorage.getItem('qldg_token') || ''
  return token.split(':')[2] || 'GV'
})

const roleErrorMessage = computed(() => {
  const role = userRole.value
  switch (role) {
    case 'TCQLDT':
      return 'Tài khoản Tổ chức & Quản lý đào tạo (TCQLDT) không thuộc phạm vi phân quyền lập kế hoạch dự giờ.'
    case 'GV':
      return 'Tài khoản Giảng viên (GV) không thuộc phạm vi phân quyền quản lý kế hoạch hoặc import lịch dạy.'
    case 'HEAD_OFFICE':
      return 'Tài khoản Quản trị cấp cao (Head Office) không thuộc phạm vi phân quyền cho thao tác này.'
    case 'CNBM':
    case 'TM':
      return 'Tài khoản Chủ nhiệm bộ môn / Trưởng môn không thuộc phạm vi phân quyền cho chức năng này.'
    default:
      return `Tài khoản với vai trò ${role} không thuộc phạm vi phân quyền cho chức năng này.`
  }
})

const pageName = computed(() => {
  const t = route.query.target
  if (t === 'subjects') return 'Môn & Bộ môn (Đặt tên môn học)'
  return 'Chức năng ngoài phạm vi'
})
</script>
