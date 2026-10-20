<template>
  <div>
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <h1 class="page-title">{{ editPlanId ? 'Chỉnh sửa kế hoạch dự giờ' : 'Lập kế hoạch dự giờ chính thức' }}</h1>
      </div>
      <router-link to="/plans/unscheduled" class="btn btn-outline-danger" style="font-weight: 600;">
        + KH dự giờ đột xuất
      </router-link>
    </div>

    <!-- Thanh bước -->
    <div class="stepper">
      <div class="step" :class="{ active: step === 1, done: step > 1 }"><span>1</span> Chọn lớp dự giờ</div>
      <div class="step-line"></div>
      <div class="step" :class="{ active: step === 2 }"><span>2</span> Phân công người dự</div>
    </div>

    <!-- BƯỚC 1: CHỌN LỚP (UC-09) -->
    <div v-if="step === 1">
      <QuotaWidget :quota="quota" />

      <div class="card filter-bar">
        <div class="form-group">
          <label class="form-label">Kỳ học</label>
          <select v-model="selectedSemesterId" class="form-control" @change="onFilterChange">
            <option :value="null">-- Chọn kỳ học --</option>
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name || (s.term + ' ' + s.year) }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Block</label>
          <select v-model="selectedBlock" class="form-control" @change="onFilterChange">
            <option :value="0">-- Tất cả block --</option>
            <option :value="1">Block 1</option>
            <option :value="2">Block 2</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Bộ môn</label>
          <select v-model="selectedFacultyId" class="form-control" @change="onFilterChange">
            <option :value="null">-- Chọn bộ môn --</option>
            <option v-for="f in myFaculties" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>
        <button class="btn btn-primary" @click="fetchClassifications">Xem danh sách lớp</button>
      </div>

      <div v-if="classified" class="card" style="padding: 0; overflow: hidden;">
        <!-- Tabs 3 nhóm lớp -->
        <div class="tabs">
          <button
            v-for="t in tabs"
            :key="t.key"
            class="tab"
            :class="[{ active: activeTab === t.key }, 'tab-' + t.key]"
            @click="activeTab = t.key"
          >
            {{ t.label }} <span class="tab-count">{{ t.list.length }}</span>
          </button>
        </div>

        <div class="tab-toolbar">
          <div class="tab-hint">{{ currentTab.hint }}</div>
          <input v-model="classSearch" class="form-control" placeholder="Tìm mã lớp, môn, giảng viên..." style="max-width: 280px;" />
        </div>

        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th v-if="activeTab !== 'planned'" style="width: 44px;">
                  <input type="checkbox" :checked="allVisibleChecked" :disabled="!pagedRows.length" @change="toggleAllVisible($event.target.checked)" />
                </th>
                <th>Mã lớp</th>
                <th>Môn học</th>
                <th>Bộ môn</th>
                <th>Giảng viên</th>
                <th v-if="activeTab !== 'planned'">Số buổi</th>
                <th>{{ activeTab === 'planned' ? 'Thuộc kế hoạch' : 'Ghi chú' }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="pagedRows.length === 0">
                <td colspan="7" class="empty-row">Không có lớp nào.</td>
              </tr>
              <tr
                v-for="c in pagedRows"
                :key="c.teachingId"
                :class="{ 'row-selected': selectedTeachingIds.includes(c.teachingId), 'row-muted': activeTab === 'planned' }"
                @click="activeTab !== 'planned' && toggleOne(c.teachingId)"
              >
                <td v-if="activeTab !== 'planned'" @click.stop>
                  <input type="checkbox" v-model="selectedTeachingIds" :value="c.teachingId" />
                </td>
                <td style="font-weight: 600;">{{ c.classCode }}</td>
                <td>{{ c.subjectCode }} – {{ c.subjectName }}</td>
                <td><span class="badge badge-gray">{{ c.facultyName || 'Bộ môn' }}</span></td>
                <td>{{ c.teacherName }} <span class="muted">({{ c.teacherAccount }})</span></td>
                <td v-if="activeTab !== 'planned'">{{ c.sessionCount }}</td>
                <td>
                  <span v-if="activeTab === 'mandatory'" class="badge badge-red">{{ c.reasonBadge || 'Bắt buộc' }}</span>
                  <span v-else-if="activeTab === 'others'" class="muted">—</span>
                  <span v-else class="badge badge-gray">{{ c.existingPlanCode }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Phân trang 4 lớp 1 trang -->
        <div v-if="totalPages > 1" class="pagination-bar">
          <div class="pagination-info">
            Hiển thị <strong>{{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, filteredList.length) }}</strong> trên tổng <strong>{{ filteredList.length }}</strong> lớp
          </div>
          <div class="pagination-controls">
            <button class="pagination-btn" :disabled="currentPage <= 1" @click="currentPage--">« Trước</button>
            <button
              v-for="p in totalPages"
              :key="p"
              class="pagination-btn"
              :class="{ active: currentPage === p }"
              @click="currentPage = p"
            >
              {{ p }}
            </button>
            <button class="pagination-btn" :disabled="currentPage >= totalPages" @click="currentPage++">Sau »</button>
          </div>
        </div>
      </div>

      <div v-if="classified" class="sticky-footer">
        <div style="font-size: 14px;">
          Đã chọn <strong style="color: var(--primary);">{{ selectedTeachingIds.length }}</strong> lớp
          <span class="muted" style="margin-left: 8px;">({{ selectedMandatoryCount }}/{{ mandatoryList.length }} lớp bắt buộc)</span>
        </div>
        <button class="btn btn-primary" :disabled="selectedTeachingIds.length === 0" @click="proceedToStep2">
          Tiếp tục: Phân công →
        </button>
      </div>
    </div>

    <!-- BƯỚC 2: PHÂN CÔNG (UC-10, UC-11) -->
    <div v-if="step === 2">
      <QuotaWidget :quota="dynamicQuota" />

      <div class="card" style="padding: 16px 20px; margin-bottom: 16px;">
        <div class="assign-header">
          <div>
            <strong>{{ planItems.length }}</strong> lớp được chọn
            <span class="muted"> · Đã phân công đủ: </span>
            <strong :style="{ color: completeCount === planItems.length ? 'var(--success)' : 'var(--primary)' }">
              {{ completeCount }}/{{ planItems.length }}
            </strong>
          </div>
          <button class="btn btn-secondary" style="font-size: 12px;" @click="step = 1">← Chọn lại lớp</button>
        </div>

        <!-- Danh sách thẻ lớp (phân trang 4 lớp 1 trang nếu nhiều) -->
        <div
          v-for="item in pagedPlanItems"
          :key="item.teachingId"
          class="class-card"
          :class="{ complete: isComplete(item), invalid: submitAttempted && !isComplete(item) }"
        >
          <div class="class-card-head">
            <div>
              <span class="class-code">{{ item.classCode }}</span>
              <span class="muted"> · {{ item.subjectCode }}</span>
            </div>
            <div style="font-size: 13px;">GV được dự: <strong>{{ item.teacherName }}</strong></div>
          </div>

          <div class="class-card-body">
            <div>
              <div class="slot-label">Buổi dự <span class="req">*</span></div>
              <select v-model="item.selectedSessionId" class="form-control" style="font-size: 12px;" @change="onSessionChange(item)">
                <option :value="null">-- Chọn buổi --</option>
                <option v-for="s in item.availableSessions" :key="s.sessionId" :value="s.sessionId">{{ s.label }}</option>
              </select>
            </div>

            <!-- Phân công GV 1, 2, 3 qua Dropdown trực tiếp hoặc tìm kiếm -->
            <div v-for="slot in [1, 2, 3]" :key="slot">
              <div class="slot-label">GV {{ slot }} <span v-if="slot < 3" class="req">*</span><span v-else class="muted">(tùy chọn)</span></div>

              <!-- Nếu đã chọn GV -->
              <div v-if="item['inspector' + slot]" class="assigned-chip">
                <span>{{ item['inspector' + slot].account }}</span>
                <span class="muted" style="font-size: 11px; font-weight: normal;" v-if="item['inspector' + slot].fullName">
                  ({{ item['inspector' + slot].fullName }})
                </span>
                <button type="button" class="chip-remove" title="Bỏ chọn" @click="removeInspector(item, slot)">✕</button>
              </div>

              <!-- Nếu chưa chọn: Dropdown chọn nhanh GV bộ môn hoặc Tìm GV chéo -->
              <div v-else class="slot-select-box">
                <select
                  class="form-control"
                  style="font-size: 12px;"
                  :class="{ 'is-invalid': submitAttempted && slot < 3 && !item['inspector' + slot] }"
                  @change="onInspectorSelect(item, slot, $event)"
                >
                  <option value="">-- Chọn GV {{ slot }} --</option>
                  <option v-for="ins in facultyInspectors" :key="ins.id" :value="ins.id">
                    {{ ins.account }} - {{ ins.fullName }}
                  </option>
                  <option value="__SEARCH_OTHER__">🔍 Tìm GV khác (chéo bộ môn/cơ sở)...</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- Phân trang Step 2 nếu > 4 lớp -->
        <div v-if="totalStep2Pages > 1" class="pagination-bar" style="border-top: none; padding: 12px 0;">
          <div class="pagination-info">
            Trang <strong>{{ step2Page }}</strong> / <strong>{{ totalStep2Pages }}</strong>
          </div>
          <div class="pagination-controls">
            <button class="pagination-btn" :disabled="step2Page <= 1" @click="step2Page--">« Trước</button>
            <button
              v-for="p in totalStep2Pages"
              :key="p"
              class="pagination-btn"
              :class="{ active: step2Page === p }"
              @click="step2Page = p"
            >
              {{ p }}
            </button>
            <button class="pagination-btn" :disabled="step2Page >= totalStep2Pages" @click="step2Page++">Sau »</button>
          </div>
        </div>
      </div>

      <div class="sticky-footer" style="justify-content: flex-end; gap: 12px;">
        <button class="btn btn-secondary" :disabled="saving" @click="savePlan('DRAFT')">Lưu nháp</button>
        <button class="btn btn-primary" :disabled="saving" @click="promptSubmit">Gửi trình duyệt →</button>
      </div>
    </div>

    <InspectorSearchModal :is-open="modalOpen" @close="modalOpen = false" @select="onCrossInspectorSelected" />

    <div v-if="showConfirmSubmit" class="modal-overlay" @click.self="showConfirmSubmit = false">
      <div class="modal-content" style="max-width: 440px;">
        <div class="modal-header">
          <div class="modal-title">Xác nhận gửi kế hoạch dự giờ</div>
          <button class="btn-icon" @click="showConfirmSubmit = false">✕</button>
        </div>
        <div class="modal-body" style="font-size: 13px;">
          Kế hoạch chuyển sang <strong>CHỜ DUYỆT</strong> và gửi tới Trưởng ban Đào tạo (TBĐT). Xác nhận gửi?
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showConfirmSubmit = false">Xem lại</button>
          <button class="btn btn-primary" @click="executeSubmit">Xác nhận gửi</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'
import QuotaWidget from '../components/QuotaWidget.vue'
import InspectorSearchModal from '../components/InspectorSearchModal.vue'

const router = useRouter()
const route = useRoute()
const editPlanId = route.query.editId ? Number(route.query.editId) : null

const step = ref(1)
const semesters = ref([])
const myFaculties = ref([])
const selectedSemesterId = ref(null)
const selectedBlock = ref(0) // Mặc định: -- Tất cả block --
const selectedFacultyId = ref(null) // Mặc định: -- Chọn bộ môn --

const quota = ref({ totalTeachers: 0, requiredQuota: 0, plannedTeachers: 0, percent: 0 })
const mandatoryList = ref([])
const othersList = ref([])
const alreadyPlannedList = ref([])
const selectedTeachingIds = ref([])
const classified = ref(false)
const activeTab = ref('mandatory')
const classSearch = ref('')

// Phân trang 4 lớp 1 trang
const pageSize = 4
const currentPage = ref(1)
const step2Page = ref(1)

const facultyInspectors = ref([])
const planItems = ref([])
const saving = ref(false)
const submitAttempted = ref(false)
const showConfirmSubmit = ref(false)

const modalOpen = ref(false)
const targetItem = ref(null)
const targetSlot = ref(1)

const tabs = computed(() => [
  { key: 'mandatory', label: 'Bắt buộc dự giờ', list: mandatoryList.value, hint: 'GV dạy môn lần đầu hoặc nghỉ dạy môn 6 kỳ liên tiếp. Mặc định đã chọn.' },
  { key: 'others', label: 'Chọn thêm', list: othersList.value, hint: 'Các lớp còn lại, tích chọn nếu muốn đưa vào kế hoạch.' },
  { key: 'planned', label: 'Đã có kế hoạch', list: alreadyPlannedList.value, hint: 'Lớp đã nằm trong kế hoạch khác, chỉ xem.' }
])
const currentTab = computed(() => tabs.value.find(t => t.key === activeTab.value))

const filteredList = computed(() => {
  const q = classSearch.value.trim().toLowerCase()
  const list = currentTab.value.list
  if (!q) return list
  return list.filter(c =>
    [c.classCode, c.subjectCode, c.subjectName, c.teacherName, c.teacherAccount, c.facultyName]
      .some(v => (v || '').toLowerCase().includes(q))
  )
})

const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})

const allVisibleChecked = computed(() =>
  pagedRows.value.length > 0 && pagedRows.value.every(c => selectedTeachingIds.value.includes(c.teachingId))
)
const selectedMandatoryCount = computed(() =>
  mandatoryList.value.filter(c => selectedTeachingIds.value.includes(c.teachingId)).length
)

watch(activeTab, () => {
  classSearch.value = ''
  currentPage.value = 1
})

watch(classSearch, () => {
  currentPage.value = 1
})

onMounted(async () => {
  try {
    const me = await api.getMe()
    myFaculties.value = me.faculties || []
    semesters.value = await api.getSemesters()
    selectedSemesterId.value = null // Mặc định: -- Chọn kỳ học --
    await fetchClassifications()
  } catch (e) {
    console.error(e)
  }
})

function onFilterChange() {
  currentPage.value = 1
  fetchClassifications()
}

async function fetchClassifications() {
  try {
    const res = await api.getClassGroups(selectedSemesterId.value, selectedBlock.value, selectedFacultyId.value)
    quota.value = res.quota
    mandatoryList.value = res.mandatory || []
    othersList.value = res.others || []
    alreadyPlannedList.value = res.alreadyPlanned || []
    selectedTeachingIds.value = mandatoryList.value.map(c => c.teachingId)
    activeTab.value = 'mandatory'
    classified.value = true
    currentPage.value = 1
  } catch (e) {
    toast.error('Lỗi tải danh sách lớp: ' + e.message)
  }
}

function toggleOne(id) {
  const i = selectedTeachingIds.value.indexOf(id)
  if (i >= 0) selectedTeachingIds.value.splice(i, 1)
  else selectedTeachingIds.value.push(id)
}

function toggleAllVisible(checked) {
  const ids = new Set(selectedTeachingIds.value)
  pagedRows.value.forEach(c => (checked ? ids.add(c.teachingId) : ids.delete(c.teachingId)))
  selectedTeachingIds.value = [...ids]
}

async function proceedToStep2() {
  step.value = 2
  step2Page.value = 1
  try {
    facultyInspectors.value = await api.getInspectorsInFaculty(selectedFacultyId.value)
  } catch {
    facultyInspectors.value = []
  }
  const chosen = [...mandatoryList.value, ...othersList.value]
    .filter(c => selectedTeachingIds.value.includes(c.teachingId))

  planItems.value = await Promise.all(chosen.map(async c => {
    const sessions = await api.getValidSessions(c.teachingId)
    const first = sessions[0]
    return {
      teachingId: c.teachingId,
      classCode: c.classCode,
      subjectCode: c.subjectCode,
      teacherName: c.teacherName,
      facultyId: c.facultyId,
      availableSessions: sessions,
      selectedSessionId: first ? first.sessionId : null,
      plannedDate: first ? first.date : null,
      shift: first ? first.shift : null,
      inspector1: null,
      inspector2: null,
      inspector3: null
    }
  }))
}

const totalStep2Pages = computed(() => Math.ceil(planItems.value.length / 4) || 1)
const pagedPlanItems = computed(() => {
  const start = (step2Page.value - 1) * 4
  return planItems.value.slice(start, start + 4)
})

const dynamicQuota = computed(() => {
  const planned = new Set(planItems.value.filter(it => it.inspector1).map(it => it.inspector1.id)).size
  const req = quota.value.requiredQuota || 1
  return {
    totalTeachers: quota.value.totalTeachers,
    requiredQuota: req,
    plannedTeachers: planned,
    percent: Math.min(100, Math.round((planned / req) * 100))
  }
})

const isComplete = it => !!(it.plannedDate && it.inspector1 && it.inspector2)
const completeCount = computed(() => planItems.value.filter(isComplete).length)

function onSessionChange(item) {
  const s = item.availableSessions.find(x => x.sessionId === item.selectedSessionId)
  item.plannedDate = s ? s.date : null
  item.shift = s ? s.shift : null
  toast.info(`Đã chọn buổi dự cho lớp ${item.classCode}`)
}

function onInspectorSelect(item, slot, event) {
  const val = event.target.value
  if (!val) {
    item['inspector' + slot] = null
    return
  }
  if (val === '__SEARCH_OTHER__') {
    event.target.value = ''
    openCrossSearch(item, slot)
    return
  }
  const ins = facultyInspectors.value.find(x => x.id === Number(val))
  if (ins) {
    item['inspector' + slot] = ins
    toast.success(`Đã phân công GV ${ins.account} vào lớp ${item.classCode}`)
  }
}

function removeInspector(item, slot) {
  const old = item['inspector' + slot]
  item['inspector' + slot] = null
  toast.info(`Đã bỏ phân công GV ${old ? old.account : ''}`)
}

function openCrossSearch(item, slot) {
  targetItem.value = item
  targetSlot.value = slot
  modalOpen.value = true
}

function onCrossInspectorSelected(user) {
  if (targetItem.value) {
    targetItem.value['inspector' + targetSlot.value] = user
    toast.success(`Đã phân công GV ${user.account} vào lớp ${targetItem.value.classCode}`)
  }
  modalOpen.value = false
}

async function savePlan(action) {
  saving.value = true
  const payload = {
    facultyId: selectedFacultyId.value || planItems.value[0]?.facultyId || (myFaculties.value[0]?.id ?? 1),
    semesterId: selectedSemesterId.value,
    block: selectedBlock.value || 1,
    unscheduled: false,
    items: planItems.value.map(it => ({
      teachingId: it.teachingId,
      plannedDate: it.plannedDate,
      shift: it.shift,
      inspector1Id: it.inspector1?.id ?? null,
      inspector2Id: it.inspector2?.id ?? null,
      inspector3Id: it.inspector3?.id ?? null
    }))
  }
  try {
    if (action === 'DRAFT') {
      await api.savePlanDraft(payload)
      toast.success('Đã lưu bản nháp kế hoạch dự giờ thành công!')
    } else {
      await api.submitPlan(payload, editPlanId)
      toast.success('Kế hoạch dự giờ đã được gửi trình duyệt thành công!')
    }
    setTimeout(() => {
      router.push('/plans')
    }, 600)
  } catch (err) {
    toast.error('Lỗi: ' + err.message)
  } finally {
    saving.value = false
  }
}

function promptSubmit() {
  submitAttempted.value = true
  const bad = planItems.value.find(it => !isComplete(it))
  if (bad) {
    toast.warning(`Lớp ${bad.classCode} chưa đủ buổi dự, GV1 hoặc GV2!`)
    return
  }
  showConfirmSubmit.value = true
}

async function executeSubmit() {
  showConfirmSubmit.value = false
  await savePlan('SUBMIT')
}
</script>

<style scoped>
.muted { color: var(--text-muted); }
.req { color: var(--danger); }

.stepper { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; font-size: 13px; }
.step { display: flex; align-items: center; gap: 8px; color: var(--text-muted); font-weight: 600; }
.step span {
  width: 24px; height: 24px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid var(--border); background: #FFF; font-size: 12px;
}
.step.active { color: var(--primary); }
.step.active span { background: var(--primary); border-color: var(--primary); color: #FFF; }
.step.done { color: var(--success); }
.step.done span { background: var(--success); border-color: var(--success); color: #FFF; }
.step-line { flex: 0 0 48px; height: 1px; background: var(--border); }

.filter-bar {
  display: grid; grid-template-columns: repeat(3, minmax(160px, 1fr)) auto; gap: 14px; align-items: end; padding: 16px 20px;
}
.filter-bar .form-group { margin-bottom: 0; }

.tabs { display: flex; border-bottom: 1px solid var(--border); padding: 0 12px; }
.tab {
  background: none; border: none; padding: 14px 16px; font-size: 14px; font-weight: 600; color: var(--text-muted);
  cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; display: flex; align-items: center; gap: 8px;
}
.tab-count { background: #F3F4F6; color: var(--text-muted); border-radius: 10px; padding: 1px 8px; font-size: 12px; }
.tab.active { color: var(--primary); border-bottom-color: var(--primary); }
.tab.active .tab-count { background: var(--primary-bg); color: var(--primary); }
.tab-mandatory.active { color: var(--danger); border-bottom-color: var(--danger); }
.tab-mandatory.active .tab-count { background: var(--danger-bg); color: var(--danger); }

.tab-toolbar {
  display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 12px 20px; background: #FAFAFA;
  border-bottom: 1px solid var(--border);
}
.tab-hint { font-size: 12px; color: var(--text-muted); }

.data-table tbody tr { cursor: pointer; }
.row-selected { background: var(--primary-bg); }
.row-muted { opacity: 0.65; cursor: default !important; }
.empty-row { text-align: center; padding: 28px; color: var(--text-muted); }

.sticky-footer {
  position: sticky; bottom: 0; margin-top: 16px; background: #FFF; border: 1px solid var(--border);
  border-radius: var(--radius); padding: 12px 20px; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05); z-index: 5;
}

.assign-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-size: 14px; }

.class-card {
  background: #FFF; border: 1px solid var(--border); border-left: 4px solid var(--border);
  border-radius: var(--radius); margin-bottom: 12px;
}
.class-card.complete { border-left-color: var(--success); }
.class-card.invalid { border-left-color: var(--danger); }
.class-card-head {
  display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; border-bottom: 1px solid var(--border-light, #F3F4F6);
}
.class-code { font-weight: 700; color: var(--primary); font-size: 15px; }
.class-card-body { display: grid; grid-template-columns: 1.3fr 1fr 1fr 1fr; gap: 12px; padding: 12px 16px; align-items: start; }
.slot-label { font-size: 11px; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; text-transform: uppercase; }

.assigned-chip {
  display: inline-flex; align-items: center; gap: 6px; background: var(--primary-bg); color: var(--primary);
  padding: 6px 10px; border-radius: 8px; font-size: 12px; font-weight: 600; border: 1px solid rgba(37, 99, 235, 0.2);
  width: 100%; box-sizing: border-box; justify-content: space-between;
}
.chip-remove { background: none; border: none; color: var(--danger); cursor: pointer; font-size: 12px; font-weight: bold; padding: 0 4px; }

@media (max-width: 1100px) {
  .class-card-body { grid-template-columns: 1fr 1fr; }
  .filter-bar { grid-template-columns: 1fr 1fr; }
}
</style>
