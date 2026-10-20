import { createRouter, createWebHistory } from 'vue-router'
import ImportScheduleView from '../views/ImportScheduleView.vue'
import ImportDetailView from '../views/ImportDetailView.vue'
import PlanListView from '../views/PlanListView.vue'
import CreatePlanView from '../views/CreatePlanView.vue'
import UnscheduledPlanView from '../views/UnscheduledPlanView.vue'
import PlanDetailView from '../views/PlanDetailView.vue'
import MyInspectionsView from '../views/MyInspectionsView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'

const routes = [
  {
    path: '/',
    redirect: () => {
      const token = localStorage.getItem('qldg_token') || '1:1:TBDT'
      const role = token.split(':')[2] || 'TBDT'
      return role === 'GV' ? '/my-inspections' : '/plans'
    }
  },
  { path: '/my-inspections', name: 'MyInspections', component: MyInspectionsView, meta: { title: 'Lịch dự giờ của tôi', roles: ['GV', 'CNBM', 'TM', 'TBDT', 'TCQLDT', 'HEAD_OFFICE'] } },
  { path: '/import', name: 'ImportSchedule', component: ImportScheduleView, meta: { title: 'Import lịch giảng dạy', roles: ['TCQLDT', 'TBDT', 'HEAD_OFFICE'] } },
  { path: '/import/:id', name: 'ImportDetail', component: ImportDetailView, meta: { title: 'Chi tiết đối soát lịch dạy', roles: ['TCQLDT', 'TBDT', 'HEAD_OFFICE'] } },
  { path: '/plans', name: 'PlanList', component: PlanListView, meta: { title: 'Quản lý KH dự giờ', roles: ['CNBM', 'TM', 'TBDT', 'TCQLDT', 'HEAD_OFFICE'] } },
  { path: '/plans/create', name: 'CreatePlan', component: CreatePlanView, meta: { title: 'Lập KH dự giờ chính thức', roles: ['CNBM', 'TM', 'TBDT', 'HEAD_OFFICE'] } },
  { path: '/plans/unscheduled', name: 'UnscheduledPlan', component: UnscheduledPlanView, meta: { title: 'Lập KH dự giờ đột xuất', roles: ['CNBM', 'TM', 'TBDT', 'HEAD_OFFICE'] } },
  { path: '/plans/:id', name: 'PlanDetail', component: PlanDetailView, meta: { title: 'Chi tiết kế hoạch dự giờ', roles: ['CNBM', 'TM', 'TBDT', 'TCQLDT', 'HEAD_OFFICE'] } },
  { path: '/placeholder', name: 'Placeholder', component: PlaceholderView, meta: { title: 'Chức năng ngoài phạm vi' } }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('qldg_token') || '1:1:TBDT'
  const parts = token.split(':')
  const userRole = parts[2] || 'TBDT'

  // Nếu là GV cố truy cập /plans thì tự động chuyển hướng sang /my-inspections
  if (userRole === 'GV' && (to.path === '/plans' || to.path.startsWith('/plans/'))) {
    return next({ path: '/my-inspections' })
  }

  if (to.meta && to.meta.roles) {
    if (!to.meta.roles.includes(userRole)) {
      return next({ path: '/placeholder', query: { target: 'unauthorized', reason: 'role_denied' } })
    }
  }
  next()
})
