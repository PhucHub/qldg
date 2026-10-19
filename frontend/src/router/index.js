import { createRouter, createWebHistory } from 'vue-router'
import ImportScheduleView from '../views/ImportScheduleView.vue'
import ImportDetailView from '../views/ImportDetailView.vue'
import PlanListView from '../views/PlanListView.vue'
import CreatePlanView from '../views/CreatePlanView.vue'
import UnscheduledPlanView from '../views/UnscheduledPlanView.vue'
import PlanDetailView from '../views/PlanDetailView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'

const routes = [
  { path: '/', redirect: '/plans' },
  { path: '/import', name: 'ImportSchedule', component: ImportScheduleView, meta: { title: 'Import lịch giảng dạy' } },
  { path: '/import/:id', name: 'ImportDetail', component: ImportDetailView, meta: { title: 'Chi tiết đối soát lịch dạy' } },
  { path: '/plans', name: 'PlanList', component: PlanListView, meta: { title: 'Quản lý KH dự giờ' } },
  { path: '/plans/create', name: 'CreatePlan', component: CreatePlanView, meta: { title: 'Lập KH dự giờ chính thức' } },
  { path: '/plans/unscheduled', name: 'UnscheduledPlan', component: UnscheduledPlanView, meta: { title: 'Lập KH dự giờ đột xuất' } },
  { path: '/plans/:id', name: 'PlanDetail', component: PlanDetailView, meta: { title: 'Chi tiết kế hoạch dự giờ' } },
  { path: '/placeholder', name: 'Placeholder', component: PlaceholderView, meta: { title: 'Chức năng ngoài phạm vi' } }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})
