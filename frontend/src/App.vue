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
          <!-- 1. TC&QLĐT, TBĐT, Super Admin mới thấy Import lịch dạy (UC-07) -->
          <li v-if="canViewImport" class="menu-item">
            <router-link to="/import" active-class="active">
              <span class="menu-icon">📥</span>
              <span v-if="!isSidebarCollapsed">Import lịch dạy</span>
            </router-link>
          </li>

          <!-- 2. CNBM, TM, TBĐT, HEAD_OFFICE: UC-09, UC-10, UC-11, UC-12 Lập KH dự giờ -->
          <li v-if="canCreatePlan" class="menu-item">
            <router-link to="/plans/create" active-class="active">
              <span class="menu-icon">✍️</span>
              <span v-if="!isSidebarCollapsed">Lập KH dự giờ</span>
            </router-link>
          </li>

          <!-- 3. CNBM, TM, TBĐT, TC&QLĐT, Super Admin thấy Quản lý KH dự giờ (UC-13) -->
          <li v-if="canViewPlans" class="menu-item">
            <router-link to="/plans" active-class="active">
              <span class="menu-icon">📋</span>
              <span v-if="!isSidebarCollapsed">Quản lý KH dự giờ</span>
            </router-link>
          </li>

          <!-- Thông báo khi tài khoản GV đăng nhập (Test Case 1.4) -->
          <li v-if="currentUser.role === 'GV' && !currentUser.headOffice" style="padding: 14px 12px; font-size: 12px; color: var(--text-muted); line-height: 1.5; background: #F9FAFB; border-radius: var(--radius); border: 1px dashed var(--border);">
            <span v-if="!isSidebarCollapsed">Tài khoản GV không có menu trong phân hệ Lập KH &amp; Import</span>
            <span v-else title="Tài khoản GV không có menu trong phân hệ Lập KH & Import">ℹ️</span>
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
                <div class="user-role" style="display: flex; align-items: center; gap: 6px;">
                  <span>{{ currentUser.headOffice ? 'Super Admin' : currentUser.role }}</span>
                  <span>–</span>
                  <!-- Dropdown chuyển đổi 14 cơ sở (/auth/campus) cho Super Admin (UC-01 & HDSD 6.1) -->
                  <select
                    v-if="currentUser.headOffice"
                    v-model="selectedCampusCode"
                    class="form-control"
                    style="padding: 1px 6px; font-size: 11px; height: 22px; width: auto; font-weight: 500; cursor: pointer; border-color: var(--primary);"
                    title="Chuyển đổi cơ sở làm việc toàn quốc (/auth/campus)"
                    @change="onCampusChange"
                  >
                    <option v-for="c in ALL_CAMPUSES" :key="c.code" :value="c.code">
                      {{ c.name }}
                    </option>
                  </select>
                  <span v-else>{{ currentUser.campus?.name || currentUser.campusName || 'CĐ FPT Polytechnic Đà Nẵng' }}</span>
                </div>
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

const ALL_CAMPUSES = [
  { id: 1, code: 'dna', name: 'CĐ FPT Polytechnic Đà Nẵng' },
  { id: 2, code: 'hn', name: 'CĐ FPT Polytechnic Hà Nội' },
  { id: 3, code: 'hcm', name: 'CĐ FPT Polytechnic TP.HCM' },
  { id: 4, code: 'ct', name: 'CĐ FPT Polytechnic Cần Thơ' },
  { id: 5, code: 'tn', name: 'CĐ FPT Polytechnic Tây Nguyên' },
  { id: 6, code: 'hp', name: 'CĐ FPT Polytechnic Hải Phòng' },
  { id: 7, code: 'bd', name: 'CĐ FPT Polytechnic Bình Định' },
  { id: 8, code: 'th', name: 'CĐ FPT Polytechnic Thanh Hóa' },
  { id: 9, code: 'qn', name: 'CĐ FPT Polytechnic Quảng Ninh' },
  { id: 10, code: 'dn', name: 'CĐ FPT Polytechnic Đồng Nai' },
  { id: 11, code: 'vt', name: 'CĐ FPT Polytechnic Vũng Tàu' },
  { id: 12, code: 'nt', name: 'CĐ FPT Polytechnic Nha Trang' },
  { id: 13, code: 'bdu', name: 'CĐ FPT Polytechnic Bình Dương' },
  { id: 14, code: 'gl', name: 'CĐ FPT Polytechnic Gia Lai' }
]

const selectedCampusCode = ref(localStorage.getItem('qldg_head_office_campus') || 'dna')

function onCampusChange() {
  const found = ALL_CAMPUSES.find(c => c.code === selectedCampusCode.value)
  if (found) {
    localStorage.setItem('qldg_head_office_campus', found.code)
    toast.success(`Đã chuyển đổi phạm vi làm việc sang: ${found.name} (/auth/campus)`)
  }
}

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

// Phân quyền menu theo vai trò (UC-07 -> UC-13):
// 1. TC&QLĐT, TBĐT, Super Admin: UC-07, 08 (Import lịch dạy)
const canViewImport = computed(() => {
  const r = currentUser.value.role
  return r === 'TCQLDT' || r === 'TBDT' || r === 'HEAD_OFFICE' || currentUser.value.headOffice
})

// 2. CNBM, TM, TBĐT, Super Admin: UC-09, 10, 11, 12 (Lập KH dự giờ)
const canCreatePlan = computed(() => {
  const r = currentUser.value.role
  return r === 'CNBM' || r === 'TM' || r === 'TBDT' || r === 'HEAD_OFFICE' || currentUser.value.headOffice
})

// 3. CNBM, TM, TBĐT, TC&QLĐT, Super Admin: UC-13 (Quản lý KH dự giờ)
const canViewPlans = computed(() => {
  const r = currentUser.value.role
  return r === 'CNBM' || r === 'TM' || r === 'TBDT' || r === 'TCQLDT' || r === 'HEAD_OFFICE' || currentUser.value.headOffice
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
