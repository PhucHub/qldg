<template>
  <div class="toast-container">
    <transition-group name="toast-anim">
      <div
        v-for="item in toastState.toasts"
        :key="item.id"
        class="toast-item"
        :class="'toast-' + item.type"
      >
        <div class="toast-icon-wrap">
          <!-- Success (Checkmark) -->
          <svg v-if="item.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <!-- Error / Delete (Cross) -->
          <svg v-else-if="item.type === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
          <!-- Warning (Exclamation in circle) -->
          <svg v-else-if="item.type === 'warning'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <!-- Info -->
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
          </svg>
        </div>
        <div class="toast-text">{{ item.message }}</div>
        <button class="toast-close-btn" title="Đóng" @click="toast.remove(item.id)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { toast, toastState } from '../services/toast.js'
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 99999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  min-width: 320px;
  max-width: 520px;
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 10px 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 12px;
  box-sizing: border-box;
}

.toast-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.toast-success .toast-icon-wrap {
  background: #ECFDF5;
  color: #10B981;
}

.toast-error .toast-icon-wrap {
  background: #FEF2F2;
  color: #EF4444;
}

.toast-warning .toast-icon-wrap {
  background: #FFFBEB;
  color: #F59E0B;
}

.toast-info .toast-icon-wrap {
  background: #EFF6FF;
  color: #3B82F6;
}

.toast-text {
  font-size: 13px;
  font-weight: 500;
  color: #1F2937;
  flex: 1;
  line-height: 1.4;
}

.toast-close-btn {
  background: transparent;
  border: none;
  color: #9CA3AF;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.15s;
}

.toast-close-btn:hover {
  color: #4B5563;
  background: #F3F4F6;
}

/* Animations */
.toast-anim-enter-active,
.toast-anim-leave-active {
  transition: all 0.25s ease;
}

.toast-anim-enter-from {
  opacity: 0;
  transform: translateY(-20px) scale(0.96);
}

.toast-anim-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.96);
}
</style>
