<template>
  <div class="card" style="padding: 16px 20px; margin-bottom: 20px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
      <div>
        <div style="font-weight: 700; font-size: 14px;">
          Chỉ tiêu dự giờ kỳ &amp; block (15%)
        </div>
        <div style="font-size: 13px; margin-top: 2px;">
          Đã đạt: <span :style="{ color: isMet ? 'var(--success)' : 'var(--primary)' }">{{ quota.plannedTeachers }}</span> / {{ quota.requiredQuota }} giảng viên
        </div>
      </div>
      <span class="badge" :class="isMet ? 'badge-green' : 'badge-blue'">
        {{ isMet ? 'Đạt chỉ tiêu' : 'Đang thực hiện' }}
      </span>
    </div>

    <div style="height: 8px; background: #E5E7EB; border-radius: 4px; overflow: hidden;">
      <div
        :style="{
          width: Math.min(100, quota.percent) + '%',
          height: '100%',
          background: isMet ? 'var(--success)' : 'var(--primary)',
          transition: 'width 0.2s'
        }"
      ></div>
    </div>

    <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); margin-top: 6px;">
      <span>Tiến độ: {{ quota.percent }}%</span>
      <span>Tối thiểu 15% (làm tròn lên): {{ quota.requiredQuota }} GV</span>
    </div>
    <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">
      Mẫu số: <strong>{{ quota.totalTeachers }}</strong> GV có lớp dạy
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  quota: {
    type: Object,
    default: () => ({ totalTeachers: 0, requiredQuota: 0, plannedTeachers: 0, percent: 0 })
  }
})

const isMet = computed(() =>
  props.quota.plannedTeachers >= props.quota.requiredQuota && props.quota.requiredQuota > 0
)
</script>
