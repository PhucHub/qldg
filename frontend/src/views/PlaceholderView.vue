<template>
  <div class="card empty-state" style="max-width: 600px; margin: 40px auto; padding: 40px 20px;">
    <div class="empty-icon">{{ isForbidden ? '⛔' : '🚧' }}</div>
    <div class="empty-title">{{ isForbidden ? 'Không có quyền truy cập' : 'Chức năng ngoài phạm vi đảm nhận' }}</div>
    <p style="font-size: 13px; color: var(--text-muted); margin: 8px 0 20px 0;">
      <template v-if="isForbidden">
        Tài khoản Giảng viên không thuộc phạm vi phân quyền quản lý kế hoạch hoặc import lịch dạy.
      </template>
      <template v-else>
        Trang "{{ pageName }}" do nhóm khác phụ trách (quản trị hệ thống, dữ liệu nền hoặc phiếu đánh giá/báo cáo).
      </template>
    </p>
    <router-link v-if="!isForbidden" to="/plans" class="btn btn-primary">
      Quay về Kế hoạch dự giờ
    </router-link>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isForbidden = computed(() => route.query.target === 'unauthorized')
const pageName = computed(() => {
  const t = route.query.target
  if (t === 'subjects') return 'Môn & Bộ môn (Đặt tên môn học)'
  return 'Chức năng ngoài phạm vi'
})
</script>
