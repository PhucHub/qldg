<template>
  <div>
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <div class="page-subtitle">Kế hoạch dự giờ / Lập kế hoạch</div>
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
      <div class="card filter-bar">
        <div class="form-group">
          <label class="form-label">Kỳ học</label>
          <select v-model="selectedSemesterId" class="form-control">
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name || (s.term + ' ' + s.year) }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Block</label>
          <select v-model="selectedBlock" class="form-control">
            <option :value="1">Block 1</option>
            <option :value="2">Block 2</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Bộ môn</label>
          <select v-model="selectedFacultyId" class="form-control">
            <option v-for="f in myFaculties" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>
        <button class="btn btn-primary" @click="fetchClassifications">Xem danh sách lớp</button>
      </div>

      <QuotaWidget :quota="quota" />

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
                  <input type="checkbox" :checked="allVisibleChecked" :disabled="!visibleRows.length" @change="toggleAllVisible($event.target.checked)" />
                </th>
                <th>Mã lớp</th>
                <th>Môn học</th>
                <th>Giảng viên</th>
                <th v-if="activeTab !== 'planned'">Số buổi</th>
                <th>{{ activeTab === 'planned' ? 'Thuộc kế hoạch' : 'Ghi chú' }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="visibleRows.length === 0">
                <td colspan="6" class="empty-row">Không có lớp nào.</td>
              </tr>
              <tr
                v-for="c in visibleRows"
                :key="c.teachingId"
                :class="{ 'row-selected': selectedTeachingIds.includes(c.teachingId), 'row-muted': activeTab === 'planned' }"
                @click="activeTab !== 'planned' && toggleOne(c.teachingId)"
              >
                <td v-if="activeTab !== 'planned'" @click.stop>
                  <input type="checkbox" v-model="selectedTeachingIds" :value="c.teachingId" />
                </td>
                <td style="font-weight: 600;">{{ c.classCode }}</td>
                <td>{{ c.subjectCode }} – {{ c.subjectName }}</td>
                <td>{{ c.teacherName }} <span class="muted">({{ c.teacherAccount }})</span></td>
                <td v-if="activeTab !== 'planned'">{{ c.sessionCount }}</td>
                <td>
                  <span v-if="activeTab === 'mandatory'" class="badge badge-red">{{ c.reasonBadge }}</span>
                  <span v-else-if="activeTab === 'others'" class="muted">—</span>
                  <span v-else class="badge badge-gray">{{ c.existingPlanCode }}</span>
                </td>
              </tr>
            </tbody>
          </table>
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
    <div class="assign-layout">
      <!-- Trái: GV bộ môn để kéo -->
      <div class="card inspector-panel">
        <div class="card-title" style="margin-bottom: 4px;">Giảng viên bộ môn</div>
        <div class="muted" style="font-size: 11px; margin-bottom: 10px;">Kéo thẻ vào ô GV của lớp.</div>
        <input v-model="inspectorSearch" class="form-control" placeholder="Tìm tên / account..." style="font-size: 12px; margin-bottom: 10px;" />
        <div class="inspector-list">
          <div
            v-for="ins in filteredFacultyInspectors"
            :key="ins.id"
            class="draggable-card"
            draggable="true"
            @dragstart="onDragStart($event, ins)"
          >
            <div style="font-weight: 600; font-size: 13px;">{{ ins.account }}</div>
            <div class="muted" style="font-size: 11px;">{{ ins.fullName }}</div>
          </div>
        </div>
      </div>

      <!-- Phải: mỗi lớp 1 thẻ -->
      <div>
        <div class="assign-header">
          <div>
            <strong>{{ planItems.length }}</strong> lớp
            <span class="muted"> · Đã phân công đủ </span>
            <strong :style="{ color: completeCount === planItems.length ? 'var(--success)' : 'var(--primary)' }">
              {{ completeCount }}/{{ planItems.length }}
            </strong>
          </div>
          <button class="btn btn-secondary" style="font-size: 12px;" @click="step = 1">← Chọn lại lớp</button>
        </div>

        <div
          v-for="item in planItems"
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

            <div v-for="slot in [1, 2, 3]" :key="slot">
              <div class="slot-label">GV {{ slot }} <span v-if="slot < 3" class="req">*</span><span v-else class="muted">(tùy chọn)</span></div>
              <div
                class="drop-target-box"
                :class="{ 'has-value': item['inspector' + slot], 'is-invalid': submitAttempted && slot < 3 && !item['inspector' + slot] }"
                @dragover.prevent
                @drop="onDropInspector(item, slot, $event)"
              >
                <div v-if="item['inspector' + slot]" class="assigned-chip">
                  <span>{{ item['inspector' + slot].account }}</span>
                  <button class="chip-remove" title="Bỏ" @click="item['inspector' + slot] = null">✕</button>
                </div>
                <div v-else class="drop-placeholder">
                  Kéo thả hoặc <button class="btn-link" @click="openCrossSearch(item, slot)">tìm</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="sticky-footer" style="justify-content: flex-end; gap: 12px;">
          <button class="btn btn-secondary" :disabled="saving" @click="savePlan('DRAFT')">Lưu nháp</button>
          <button class="btn btn-primary" :disabled="saving" @click="promptSubmit">Gửi trình duyệt →</button>
        </div>
      </div>
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
import QuotaWidget from '../components/QuotaWidget.vue'
import InspectorSearchModal from '../components/InspectorSearchModal.vue'

const router = useRouter()
const route = useRoute()
const editPlanId = route.query.editId ? Number(route.query.editId) : null

const step = ref(1)
const semesters = ref([])
const myFaculties = ref([])
const selectedSemesterId = ref(1)
const selectedBlock = ref(1)
const selectedFacultyId = ref(1)

const quota = ref({ totalTeachers: 0, requiredQuota: 0, plannedTeachers: 0, percent: 0 })
const mandatoryList = ref([])
const othersList = ref([])
const alreadyPlannedList = ref([])
const selectedTeachingIds = ref([])
const classified = ref(false)
const activeTab = ref('mandatory')
const classSearch = ref('')

const facultyInspectors = ref([])
const inspectorSearch = ref('')
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

const visibleRows = computed(() => {
  const q = classSearch.value.trim().toLowerCase()
  const list = currentTab.value.list
  if (!q) return list
  return list.filter(c =>
    [c.classCode, c.subjectCode, c.subjectName, c.teacherName, c.teacherAccount]
      .some(v => (v || '').toLowerCase().includes(q))
  )
})

const allVisibleChecked = computed(() =>
  visibleRows.value.length > 0 && visibleRows.value.every(c => selectedTeachingIds.value.includes(c.teachingId))
)
const selectedMandatoryCount = computed(() =>
  mandatoryList.value.filter(c => selectedTeachingIds.value.includes(c.teachingId)).length
)

watch(activeTab, () => { classSearch.value = '' })

onMounted(async () => {
  try {
    const me = await api.getMe()
    myFaculties.value = me.faculties || []
    if (myFaculties.value.length) selectedFacultyId.value = myFaculties.value[0].id
    semesters.value = await api.getSemesters()
    if (semesters.value.length) selectedSemesterId.value = semesters.value[0].id
    await fetchClassifications()
  } catch (e) {
    console.error(e)
  }
})

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
  } catch (e) {
    alert('Lỗi tải danh sách lớp: ' + e.message)
  }
}

function toggleOne(id) {
  const i = selectedTeachingIds.value.indexOf(id)
  if (i >= 0) selectedTeachingIds.value.splice(i, 1)
  else selectedTeachingIds.value.push(id)
}

function toggleAllVisible(checked) {
  const ids = new Set(selectedTeachingIds.value)
  visibleRows.value.forEach(c => (checked ? ids.add(c.teachingId) : ids.delete(c.teachingId)))
  selectedTeachingIds.value = [...ids]
}

async function proceedToStep2() {
  step.value = 2
  facultyInspectors.value = await api.getInspectorsInFaculty(selectedFacultyId.value)
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

const filteredFacultyInspectors = computed(() => {
  const q = inspectorSearch.value.toLowerCase().trim()
  if (!q) return facultyInspectors.value
  return facultyInspectors.value.filter(i => i.account.toLowerCase().includes(q) || i.fullName.toLowerCase().includes(q))
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
}

function onDragStart(e, inspector) {
  e.dataTransfer.setData('text/plain', JSON.stringify(inspector))
}

function onDropInspector(item, slot, e) {
  const raw = e.dataTransfer.getData('text/plain')
  if (!raw) return
  try {
    item['inspector' + slot] = JSON.parse(raw)
  } catch (err) {
    console.error('Lỗi nhận thả giảng viên:', err)
  }
}

function openCrossSearch(item, slot) {
  targetItem.value = item
  targetSlot.value = slot
  modalOpen.value = true
}

function onCrossInspectorSelected(user) {
  if (targetItem.value) targetItem.value['inspector' + targetSlot.value] = user
}

async function savePlan(action) {
  saving.value = true
  const payload = {
    facultyId: selectedFacultyId.value,
    semesterId: selectedSemesterId.value,
    block: selectedBlock.value,
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
      alert('Đã lưu bản nháp')
    } else {
      await api.submitPlan(payload, editPlanId)
      alert('Đã gửi trình duyệt tới TBĐT')
    }
    router.push('/plans')
  } catch (err) {
    alert('Lỗi: ' + err.message)
  } finally {
    saving.value = false
  }
}

function promptSubmit() {
  submitAttempted.value = true
  const bad = planItems.value.find(it => !isComplete(it))
  if (bad) {
    alert(`Lớp ${bad.classCode} chưa đủ buổi dự, GV1 và GV2.`)
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

.assign-layout { display: grid; grid-template-columns: 240px 1fr; gap: 20px; align-items: flex-start; }
.inspector-panel { padding: 16px; position: sticky; top: 16px; }
.inspector-list { display: flex; flex-direction: column; gap: 6px; max-height: 60vh; overflow-y: auto; }
.assign-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 14px; }

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
.class-card-body { display: grid; grid-template-columns: 1.3fr 1fr 1fr 1fr; gap: 12px; padding: 12px 16px; }
.slot-label { font-size: 11px; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; text-transform: uppercase; }

.draggable-card {
  padding: 8px 12px; background: #FFF; border: 1px solid var(--border); border-radius: var(--radius); cursor: grab;
}
.draggable-card:hover { border-color: var(--primary); background: var(--primary-bg); }

.drop-target-box {
  min-height: 36px; border: 1px dashed var(--border); border-radius: var(--radius); padding: 4px; background: #FAFAFA;
  display: flex; align-items: center; justify-content: center;
}
.drop-target-box.is-invalid { border-color: var(--danger); background: var(--danger-bg); }
.drop-target-box.has-value { border-style: solid; border-color: var(--primary); background: #FFF; }
.assigned-chip {
  display: inline-flex; align-items: center; gap: 6px; background: var(--primary-bg); color: var(--primary);
  padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.chip-remove { background: none; border: none; color: var(--danger); cursor: pointer; font-size: 11px; }
.drop-placeholder { font-size: 11px; color: var(--text-muted); }
.btn-link {
  background: none; border: none; color: var(--primary); font-size: 11px; font-weight: 600; cursor: pointer; text-decoration: underline; padding: 0;
}

@media (max-width: 1100px) {
  .assign-layout { grid-template-columns: 1fr; }
  .inspector-panel { position: static; }
  .class-card-body { grid-template-columns: 1fr 1fr; }
  .filter-bar { grid-template-columns: 1fr 1fr; }
}
</style>
