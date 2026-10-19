<template>
  <div v-if="devMode" class="dev-banner">
    <div style="display: flex; align-items: center; gap: 8px;">
      <span class="badge badge-gray" style="background: #111827; color: #FFF; border: none;">CHẾ ĐỘ DEV</span>
      <span>Fake Token Giả Lập Vai Trò:</span>
      <select v-model="selectedUserId" @change="switchUser">
        <option v-for="u in users" :key="u.id" :value="u.id">
          {{ u.label }}
        </option>
      </select>
    </div>
    <div style="display: flex; align-items: center; gap: 12px; font-size: 11px;">
      <span>Token hiện tại: <code style="color: #93C5FD;">{{ currentToken }}</code></span>
      <button @click="switchUser">Áp dụng</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../services/api.js'

const devMode = ref(true)
const users = ref([])
const selectedUserId = ref(1)
const currentToken = ref('')

onMounted(async () => {
  currentToken.value = localStorage.getItem('qldg_token') || '1:1:TBDT'
  try {
    const list = await api.getDevUsers()
    users.value = list
    const parts = currentToken.value.split(':')
    if (parts.length > 0) {
      selectedUserId.value = Number(parts[0])
    }
  } catch (e) {
    console.error('Lỗi tải danh sách dev users:', e)
  }
})

async function switchUser() {
  try {
    const res = await api.createDevToken(selectedUserId.value)
    localStorage.setItem('qldg_token', res.token)
    currentToken.value = res.token
    window.location.reload()
  } catch (e) {
    alert('Không thể đổi user: ' + e.message)
  }
}
</script>
