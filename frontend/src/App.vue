<template>
  <div>
    <!-- Banner Dev Fake Token -->
    <DevBanner />

    <!-- Global Toast Container (Top center, 3s) -->
    <ToastContainer />

    <div class="app-layout">
      <!-- SIDEBAR (Theo ảnh 1: nền trắng, logo, bo góc, thu gọn được) -->
      <aside class="sidebar" :class="{ collapsed: isSidebarCollapsed }">
        <div class="sidebar-header">
          <div class="sidebar-logo">
            <img src="/fpt-logo.png" alt="FPT Education Logo" style="height: 48px; max-height: 48px; width: auto; object-fit: contain;" />
          </div>
          <button class="btn-icon" :title="isSidebarCollapsed ? 'Mở rộng' : 'Thu gọn'" @click="isSidebarCollapsed = !isSidebarCollapsed">
            {{ isSidebarCollapsed ? '»' : '«' }}
          </button>
        </div>

        <ul class="sidebar-menu">
          <!-- UC-15: Lịch dự giờ của tôi (GV, TM, CNBM, TBĐT...) -->
          <li class="menu-item">
            <router-link to="/my-inspections" active-class="active">
              <span class="menu-icon">📅</span>
              <span v-if="!isSidebarCollapsed">Lịch dự giờ của tôi</span>
            </router-link>
          </li>

          <!-- TC&QLĐT, TBĐT, Super Admin mới thấy Import lịch dạy -->
          <li v-if="canViewImport" class="menu-item">
            <router-link to="/import" active-class="active">
              <span class="menu-icon">📥</span>
              <span v-if="!isSidebarCollapsed">Import lịch dạy</span>
            </router-link>
          </li>

          <!-- CNBM, TM, TBĐT mới thấy Lập KH dự giờ -->
          <li v-if="canCreatePlan" class="menu-item">
            <router-link to="/plans/create" active-class="active">
              <span class="menu-icon">✍️</span>
              <span v-if="!isSidebarCollapsed">Lập KH dự giờ</span>
            </router-link>
          </li>

          <!-- CNBM, TM, TBĐT, TC&QLĐT, Super Admin thấy Quản lý KH dự giờ -->
          <li v-if="canViewPlans" class="menu-item">
            <router-link to="/plans" active-class="active">
              <span class="menu-icon">📋</span>
              <span v-if="!isSidebarCollapsed">Quản lý KH dự giờ</span>
            </router-link>
          </li>
        </ul>
      </aside>

      <!-- MAIN CONTENT -->
      <div class="main-wrapper">
        <!-- HEADER (Theo ảnh 1 & 2: Breadcrumb, Chuông thông báo, User info, Logout) -->
        <header class="app-header">
          <div class="breadcrumb">
            <span>Kế hoạch dự giờ</span>
            <span>/</span>
            <span class="current">{{ pageTitle }}</span>
          </div>

          <div class="header-right">
            <!-- Chuông thông báo kết quả import & kế hoạch -->
            <div style="position: relative;">
              <button class="noti-btn" @click="showNotiDropdown = !showNotiDropdown">
                🔔
                <span v-if="unreadCount > 0" class="noti-badge">{{ unreadCount }}</span>
              </button>

              <!-- Dropdown thông báo -->
              <div v-if="showNotiDropdown" style="position: absolute; right: 0; top: 44px; width: 340px; background: #FFF; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: 0 4px 16px rgba(0,0,0,0.12); z-index: 1000; overflow: hidden;">
                <div style="padding: 10px 14px; background: #FAFAFA; border-bottom: 1px solid var(--border); font-weight: 600; font-size: 13px;">
                  Thông báo hệ thống
                </div>
                <div style="max-height: 280px; overflow-y: auto;">
                  <div v-if="notifications.length === 0" style="padding: 20px; text-align: center; font-size: 12px; color: var(--text-muted);">
                    Không có thông báo mới.
                  </div>
                  <div
                    v-for="n in notifications"
                    :key="n.id"
                    style="padding: 10px 14px; border-bottom: 1px solid var(--border-light); font-size: 12px; cursor: pointer;"
                    :style="{ background: n.read ? '#FFF' : 'var(--primary-bg)' }"
                    @click="onNotiClick(n)"
                  >
                    <div>{{ n.message }}</div>
                    <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px;">{{ formatDate(n.createdAt) }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Profile người dùng: "Xin chào, [Họ tên] ([Vai trò] – [Cơ sở])" -->
            <div class="user-profile">
              <div class="user-avatar">
                {{ userInitials }}
              </div>
              <div class="user-info">
                <div class="user-name">Xin chào, {{ currentUser.fullName || currentUser.account }}</div>
                <div class="user-role">{{ currentUser.role }} – {{ currentUser.campus?.name }}</div>
              </div>
            </div>

            <!-- Nút đăng xuất pill viền đỏ -->
            <button class="btn-logout" @click="logout">
              Đăng xuất
            </button>
          </div>
        </header>

        <!-- ROUTER VIEW -->
        <main class="page-content">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from './services/api.js'
import { toast } from './services/toast.js'
import DevBanner from './components/DevBanner.vue'
import ToastContainer from './components/ToastContainer.vue'

const route = useRoute()
const router = useRouter()

const isSidebarCollapsed = ref(false)
const currentUser = ref({})
const notifications = ref([])
const showNotiDropdown = ref(false)

const pageTitle = computed(() => route.meta?.title || 'Trang chủ')

const userInitials = computed(() => {
  const name = currentUser.value.fullName || currentUser.value.account || 'U'
  const words = name.trim().split(' ')
  if (words.length >= 2) {
    return (words[0][0] + words[words.length - 1][0]).toUpperCase()
  }
  return name.substring(0, 2).toUpperCase()
})

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

// Phân quyền menu theo vai trò (Prompt & SRS):
// TC&QLĐT, TBĐT, Super Admin: UC-07, 08 (Import lịch dạy)
const canViewImport = computed(() => {
  const r = currentUser.value.role
  return r === 'TCQLDT' || r === 'TBDT' || currentUser.value.headOffice
})

// CNBM, TM, TBĐT: UC-09, 10, 11, 12 (Lập KH dự giờ)
const canCreatePlan = computed(() => {
  const r = currentUser.value.role
  return r === 'CNBM' || r === 'TM' || r === 'TBDT' || currentUser.value.headOffice
})

// CNBM, TM, TBĐT, TC&QLĐT, Super Admin: Quản lý KH dự giờ
const canViewPlans = computed(() => {
  const r = currentUser.value.role
  return r === 'CNBM' || r === 'TM' || r === 'TBDT' || r === 'TCQLDT' || currentUser.value.headOffice
})

let initialNotiLoad = true
const seenNotiIds = new Set()

onMounted(async () => {
  try {
    currentUser.value = await api.getMe()
    loadNotifications()
    setInterval(loadNotifications, 5000)
  } catch (e) {
    console.error(e)
  }
})

async function loadNotifications() {
  try {
    const list = await api.getNotifications()
    if (!initialNotiLoad) {
      for (const n of list) {
        if (!n.read && !seenNotiIds.has(n.id)) {
          toast.info(n.message, 3000)
        }
      }
    }
    list.forEach(n => seenNotiIds.add(n.id))
    initialNotiLoad = false
    notifications.value = list
  } catch (e) {
    console.error(e)
  }
}

async function onNotiClick(n) {
  await api.markNotificationRead(n.id)
  n.read = true
  showNotiDropdown.value = false
  if (n.link) router.push(n.link)
}

function logout() {
  if (confirm('Bạn có chắc chắn muốn đăng xuất không?')) {
    localStorage.removeItem('qldg_token')
    window.location.reload()
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}
</script>
