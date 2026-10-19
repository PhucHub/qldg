<template>
  <div class="card empty-state" style="max-width: 600px; margin: 40px auto; padding: 40px 20px;">
    <div class="empty-icon">🚧</div>
    <div class="empty-title">Chức năng ngoài phạm vi đảm nhận</div>
    <p style="font-size: 13px; color: var(--text-muted); margin: 8px 0 20px 0;">
      Trang "{{ pageName }}" do nhóm khác phụ trách (quản trị hệ thống, dữ liệu nền hoặc phiếu đánh giá/báo cáo).
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
const pageName = computed(() => {
  const t = route.query.target
  if (t === 'subjects') return 'Môn & Bộ môn (Đặt tên môn học)'
  return 'Chức năng ngoài phạm vi'
})
</script>
