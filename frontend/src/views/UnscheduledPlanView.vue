<template>
  <div>
    <!-- Header trang -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <div class="page-subtitle">Kế hoạch dự giờ / Đột xuất</div>
        <div style="display: flex; align-items: center; gap: 10px;">
          <h1 class="page-title" style="color: var(--danger);">Lập kế hoạch dự giờ đột xuất</h1>
          <span class="badge badge-red" style="font-size: 13px;">MÃ -DX</span>
        </div>
      </div>
      <div>
        <router-link to="/plans" class="btn btn-secondary">Quay về danh sách</router-link>
      </div>
    </div>

    <!-- Stepper 2 bước -->
    <div class="stepper">
      <div class="step" :class="{ active: step === 1, done: step > 1 }"><span>1</span> Chọn lớp &amp; Lý do đột xuất</div>
      <div class="step-line"></div>
      <div class="step" :class="{ active: step === 2 }"><span>2</span> Phân công người dự</div>
    </div>

    <!-- BƯỚC 1: CHỌN LỚP & NHẬP LÝ DO -->
    <div v-if="step === 1">
      <!-- Bộ lọc gọn gàng trong 1 hàng -->
      <div class="card filter-bar">
        <div class="form-group">
          <label class="form-label">Kỳ học</label>
          <select v-model="selectedSemesterId" class="form-control" @change="fetchCandidates">
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name || (s.term + ' ' + s.year) }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Block</label>
          <select v-model="selectedBlock" class="form-control" @change="fetchCandidates">
            <option :value="1">Block 1</option>
            <option :value="2">Block 2</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Bộ môn</label>
          <select v-model="selectedFacultyId" class="form-control" @change="fetchCandidates">
            <option v-for="f in myFaculties" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Tìm nhanh</label>
          <input v-model="searchQuery" class="form-control" placeholder="Tìm giảng viên, mã lớp, môn..." />
        </div>
      </div>

      <div class="hint-banner">
        ⚠️ Áp dụng khi có phản ánh hoặc kiểm tra đột xuất. Hiển thị tất cả lớp của bộ môn trong block. <strong>Bắt buộc nhập lý do cho từng lớp chọn</strong>. Giảng viên được dự vẫn tính vào chỉ tiêu 15%.
      </div>

      <!-- Trạng thái tải / Trống -->
      <div v-if="loading" class="card" style="text-align: center; padding: 40px; color: var(--text-muted);">
        Đang tải danh sách lớp học phần...
      </div>

      <div v-else-if="filteredGroups.length === 0" class="empty-state card">
        <div class="empty-icon">📂</div>
        <div class="empty-title">Không tìm thấy lớp học phần phù hợp</div>
        <div class="muted" style="font-size: 13px;">Thử đổi từ khóa tìm kiếm hoặc kiểm tra lại lịch dạy đã import.</div>
      </div>

      <!-- 1 BẢNG DUY NHẤT: Gom theo Giảng viên bằng dòng nhóm, không lặp lại tiêu đề bảng -->
      <div v-else class="card" style="padding: 0; overflow: hidden;">
        <div class="table-toolbar">
          <div style="font-size: 13px; font-weight: 600;">
            Danh sách lớp học phần ({{ totalClassCount }} lớp / {{ filteredGroups.length }} giảng viên)
          </div>
          <div style="display: flex; gap: 8px;">
            <button class="btn btn-secondary" style="font-size: 12px; padding: 5px 12px;" @click="toggleSelectAllFiltered">
              {{ allFilteredSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả lớp đang hiện' }}
            </button>
          </div>
        </div>

        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 44px; text-align: center;">Chọn</th>
                <th style="width: 140px;">Mã lớp</th>
                <th>Môn học</th>
                <th style="width: 160px;">Trạng thái</th>
                <th style="min-width: 320px;">Lý do dự giờ đột xuất <span class="req">*</span></th>
              </tr>
            </thead>
            <tbody>
              <template v-for="grp in filteredGroups" :key="grp.teacherId">
                <!-- Dòng phân cách từng Giảng viên: rõ ràng, trang nhã -->
                <tr class="teacher-divider-row">
                  <td colspan="5">
                    <div class="teacher-divider-content">
                      <div class="teacher-name-box">
                        <span class="teacher-avatar">{{ grp.teacherName.charAt(0) }}</span>
                        <strong>{{ grp.teacherName }}</strong>
                        <span class="muted">({{ grp.teacherAccount }})</span>
                        <span class="badge badge-gray">{{ grp.classes.length }} lớp</span>
                      </div>
                      <button
                        type="button"
                        class="btn-text-link"
                        @click="toggleGroup(grp)"
                      >
                        {{ isGroupAllSelected(grp) ? 'Bỏ chọn GV này' : '+ Chọn cả ' + grp.classes.length + ' lớp' }}
                      </button>
                    </div>
                  </td>
                </tr>

                <!-- Các lớp của Giảng viên -->
                <tr
                  v-for="cls in grp.classes"
                  :key="cls.teachingId"
                  :class="{ 'row-selected': cls.selected }"
                  class="class-item-row"
                >
                  <td style="text-align: center;" @click.stop>
                    <input
                      type="checkbox"
                      v-model="cls.selected"
                      @change="onClassToggle(cls)"
                    />
                  </td>
                  <td>
                    <span class="class-code">{{ cls.classCode }}</span>
                  </td>
                  <td>
                    <div style="font-weight: 500;">{{ cls.subjectCode }}</div>
                    <div class="muted" style="font-size: 11px;">{{ cls.subjectName }}</div>
                  </td>
                  <td>
                    <span v-if="cls.alreadyPlanned" class="badge badge-gray" :title="'Đã thuộc kế hoạch ' + cls.existingPlanCode">
                      Đã có KH ({{ cls.existingPlanCode }})
                    </span>
                    <span v-else class="badge badge-blue">Chưa có KH</span>
                  </td>
                  <td>
                    <div class="reason-input-wrapper">
                      <input
                        v-model="cls.reason"
                        class="form-control reason-input"
                        :class="{ 'reason-error': attempted && cls.selected && !cls.reason.trim() }"
                        :placeholder="cls.selected ? 'Ghi rõ lý do (VD: Sinh viên phản ánh giảng dạy, KT đột xuất)...' : 'Tích chọn để nhập lý do'"
                        :disabled="!cls.selected"
                        @focus="cls.selected = true"
                      />
                      <span v-if="attempted && cls.selected && !cls.reason.trim()" class="reason-error-hint">
                        Bắt buộc nhập lý do
                      </span>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Thanh cố định chân trang -->
      <div v-if="!loading && teacherGroups.length > 0" class="sticky-footer">
        <div>
          Đã chọn <strong style="color: var(--danger); font-size: 15px;">{{ selectedClasses.length }}</strong> lớp đột xuất
          <span v-if="missingReasonCount > 0" class="muted" style="color: var(--danger); margin-left: 8px;">
            (⚠️ còn {{ missingReasonCount }} lớp chưa nhập lý do)
          </span>
        </div>
        <button class="btn btn-danger" :disabled="selectedClasses.length === 0" @click="proceedToAssign">
          Tiếp tục phân công người dự →
        </button>
      </div>
    </div>

    <!-- BƯỚC 2: PHÂN CÔNG NGƯỜI DỰ & GỬI DUYỆT -->
    <div v-if="step === 2">
      <div class="card" style="padding: 0; overflow: hidden; border-top: 4px solid var(--danger);">
        <div class="table-toolbar">
          <div>
            <span class="badge badge-red" style="font-size: 12px; margin-right: 8px;">ĐỘT XUẤT</span>
            <strong>Phân công dự giờ đột xuất</strong>
            <span class="muted" style="margin-left: 8px;">({{ assignItems.length }} lớp)</span>
          </div>
          <button class="btn btn-secondary" style="font-size: 12px;" @click="step = 1">← Chọn lại lớp</button>
        </div>

        <div style="padding: 16px;">
          <!-- Thẻ lớp phân công dạng Card gọn gàng -->
          <div
            v-for="item in assignItems"
            :key="item.teachingId"
            class="assign-card"
            :class="{ complete: isItemComplete(item), invalid: submitAttempted && !isItemComplete(item) }"
          >
            <div class="assign-card-head">
              <div>
                <span class="class-code">{{ item.classCode }}</span>
                <span class="muted"> · {{ item.subjectCode }}</span>
                <span class="muted" style="margin-left: 10px;">GV: <strong>{{ item.teacherName }}</strong> ({{ item.teacherAccount }})</span>
              </div>
              <span class="badge badge-red" style="max-width: 320px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" :title="item.reason">
                Lý do: {{ item.reason }}
              </span>
            </div>

            <div class="assign-card-body">
              <div>
                <div class="slot-label">Buổi dự (Lịch thật) <span class="req">*</span></div>
                <select v-model="item.selectedSessionId" class="form-control" style="font-size: 12px;" @change="onSessionChange(item)">
                  <option :value="null">-- Chọn buổi thật --</option>
                  <option v-for="s in item.availableSessions" :key="s.sessionId" :value="s.sessionId">
                    {{ s.label }}
                  </option>
                </select>
              </div>

              <!-- GV 1 -->
              <div>
                <div class="slot-label">GV 1 <span class="req">*</span></div>
                <div class="slot-box" :class="{ 'has-val': item.inspector1, 'is-invalid': submitAttempted && !item.inspector1 }">
                  <div v-if="item.inspector1" class="assigned-chip">
                    <span>{{ item.inspector1.account }}</span>
                    <button class="chip-remove" @click="item.inspector1 = null">✕</button>
                  </div>
                  <button v-else class="btn-link" @click="openSearch(item, 1)">+ Tìm gán</button>
                </div>
              </div>

              <!-- GV 2 -->
              <div>
                <div class="slot-label">GV 2 <span class="req">*</span></div>
                <div class="slot-box" :class="{ 'has-val': item.inspector2, 'is-invalid': submitAttempted && !item.inspector2 }">
                  <div v-if="item.inspector2" class="assigned-chip">
                    <span>{{ item.inspector2.account }}</span>
                    <button class="chip-remove" @click="item.inspector2 = null">✕</button>
                  </div>
                  <button v-else class="btn-link" @click="openSearch(item, 2)">+ Tìm gán</button>
                </div>
              </div>

              <!-- GV 3 -->
              <div>
                <div class="slot-label">GV 3 <span class="muted">(tùy chọn)</span></div>
                <div class="slot-box" :class="{ 'has-val': item.inspector3 }">
                  <div v-if="item.inspector3" class="assigned-chip">
                    <span>{{ item.inspector3.account }}</span>
                    <button class="chip-remove" @click="item.inspector3 = null">✕</button>
                  </div>
                  <button v-else class="btn-link" @click="openSearch(item, 3)">+ Tìm gán</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="sticky-footer" style="justify-content: flex-end; gap: 12px; margin: 0; border-radius: 0;">
          <button class="btn btn-secondary" :disabled="submitting" @click="step = 1">Xem lại lớp</button>
          <button class="btn btn-danger" :disabled="submitting" @click="submitUnscheduled">
            {{ submitting ? 'Đang gửi...' : 'Gửi duyệt KH đột xuất (-DX) →' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Tìm người dự chéo (UC-11) -->
    <InspectorSearchModal
      :is-open="modalOpen"
      @close="modalOpen = false"
      @select="onInspectorSelected"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api.js'
import InspectorSearchModal from '../components/InspectorSearchModal.vue'

const router = useRouter()
const step = ref(1)
const semesters = ref([])
const myFaculties = ref([])

const selectedSemesterId = ref(1)
const selectedBlock = ref(1)
const selectedFacultyId = ref(1)
const searchQuery = ref('')

const teacherGroups = ref([])
const loading = ref(false)
const attempted = ref(false)
const submitAttempted = ref(false)
const submitting = ref(false)

const assignItems = ref([])
const modalOpen = ref(false)
const targetItem = ref(null)
const targetSlot = ref(1)

onMounted(async () => {
  try {
    const me = await api.getMe()
    myFaculties.value = me.faculties || []
    if (myFaculties.value.length > 0) selectedFacultyId.value = myFaculties.value[0].id
    semesters.value = await api.getSemesters()
    if (semesters.value.length > 0) selectedSemesterId.value = semesters.value[0].id
    await fetchCandidates()
  } catch (e) {
    console.error(e)
  }
})

async function fetchCandidates() {
  loading.value = true
  try {
    const res = await api.getUnscheduledCandidates(
      selectedSemesterId.value,
      selectedBlock.value,
      selectedFacultyId.value
    )
    teacherGroups.value = res.map(g => ({
      ...g,
      classes: g.classes.map(c => ({ ...c, selected: false, reason: '' }))
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const filteredGroups = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return teacherGroups.value
  return teacherGroups.value
    .map(g => {
      const matchTeacher = g.teacherName.toLowerCase().includes(q) || g.teacherAccount.toLowerCase().includes(q)
      if (matchTeacher) return g
      const matchedClasses = g.classes.filter(c =>
        c.classCode.toLowerCase().includes(q) ||
        c.subjectCode.toLowerCase().includes(q) ||
        c.subjectName.toLowerCase().includes(q)
      )
      if (matchedClasses.length > 0) {
        return { ...g, classes: matchedClasses }
      }
      return null
    })
    .filter(Boolean)
})

const totalClassCount = computed(() => {
  return filteredGroups.value.reduce((acc, g) => acc + g.classes.length, 0)
})

const selectedClasses = computed(() => {
  const list = []
  teacherGroups.value.forEach(g => {
    g.classes.forEach(c => {
      if (c.selected) list.push({ ...c, teacherName: g.teacherName, teacherAccount: g.teacherAccount })
    })
  })
  return list
})

const missingReasonCount = computed(() => {
  return selectedClasses.value.filter(c => !c.reason || !c.reason.trim()).length
})

const allFilteredSelected = computed(() => {
  if (totalClassCount.value === 0) return false
  return filteredGroups.value.every(g => g.classes.every(c => c.selected))
})

function isGroupAllSelected(grp) {
  return grp.classes.length > 0 && grp.classes.every(c => c.selected)
}

function toggleGroup(grp) {
  const target = !isGroupAllSelected(grp)
  grp.classes.forEach(c => {
    c.selected = target
    if (!target) c.reason = ''
  })
}

function toggleSelectAllFiltered() {
  const target = !allFilteredSelected.value
  filteredGroups.value.forEach(g => {
    g.classes.forEach(c => {
      c.selected = target
      if (!target) c.reason = ''
    })
  })
}

function onClassToggle(cls) {
  if (!cls.selected) cls.reason = ''
}

async function proceedToAssign() {
  attempted.value = true
  if (missingReasonCount.value > 0) {
    const firstMissing = selectedClasses.value.find(c => !c.reason || !c.reason.trim())
    alert(`Lớp ${firstMissing?.classCode || ''} chưa nhập lý do đột xuất! Vui lòng điền đủ lý do.`)
    return
  }

  step.value = 2
  const items = []
  for (const c of selectedClasses.value) {
    const sessions = await api.getValidSessions(c.teachingId)
    items.push({
      teachingId: c.teachingId,
      classCode: c.classCode,
      subjectCode: c.subjectCode,
      teacherName: c.teacherName,
      teacherAccount: c.teacherAccount,
      reason: c.reason.trim(),
      availableSessions: sessions,
      selectedSessionId: sessions.length > 0 ? sessions[0].sessionId : null,
      plannedDate: sessions.length > 0 ? sessions[0].date : null,
      shift: sessions.length > 0 ? sessions[0].shift : null,
      inspector1: null,
      inspector2: null,
      inspector3: null
    })
  }
  assignItems.value = items
}

const isItemComplete = it => !!(it.plannedDate && it.inspector1 && it.inspector2)

function onSessionChange(item) {
  const s = item.availableSessions.find(x => x.sessionId === item.selectedSessionId)
  if (s) {
    item.plannedDate = s.date
    item.shift = s.shift
  } else {
    item.plannedDate = null
    item.shift = null
  }
}

function openSearch(item, slot) {
  targetItem.value = item
  targetSlot.value = slot
  modalOpen.value = true
}

function onInspectorSelected(u) {
  if (!targetItem.value) return
  if (targetSlot.value === 1) targetItem.value.inspector1 = u
  else if (targetSlot.value === 2) targetItem.value.inspector2 = u
  else if (targetSlot.value === 3) targetItem.value.inspector3 = u
}

async function submitUnscheduled() {
  submitAttempted.value = true
  for (const it of assignItems.value) {
    if (!it.inspector1 || !it.inspector2) {
      alert(`Lớp ${it.classCode} bắt buộc phải phân công cả GV1 và GV2!`)
      return
    }
    if (!it.plannedDate) {
      alert(`Lớp ${it.classCode} chưa chọn ngày dự kiến!`)
      return
    }
  }

  submitting.value = true
  const payload = {
    facultyId: selectedFacultyId.value,
    semesterId: selectedSemesterId.value,
    block: selectedBlock.value,
    unscheduled: true,
    submitAction: 'SUBMIT',
    items: assignItems.value.map(it => ({
      teachingId: it.teachingId,
      plannedDate: it.plannedDate,
      shift: it.shift,
      reason: it.reason,
      inspector1Id: it.inspector1 ? it.inspector1.id : null,
      inspector2Id: it.inspector2 ? it.inspector2.id : null,
      inspector3Id: it.inspector3 ? it.inspector3.id : null
    }))
  }

  try {
    const res = await api.submitPlan(payload)
    alert(`Đã gửi kế hoạch đột xuất ${res.code} thành công! Giảng viên được dự vẫn tính vào chỉ tiêu 15%.`)
    router.push('/plans')
  } catch (e) {
    alert('Lỗi: ' + e.message)
  } finally {
    submitting.value = false
  }
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
.step.active { color: var(--danger); }
.step.active span { background: var(--danger); border-color: var(--danger); color: #FFF; }
.step.done { color: var(--success); }
.step.done span { background: var(--success); border-color: var(--success); color: #FFF; }
.step-line { flex: 0 0 48px; height: 1px; background: var(--border); }

.filter-bar {
  display: grid; grid-template-columns: repeat(3, minmax(160px, 1fr)) 1.5fr; gap: 14px; align-items: end; padding: 14px 20px;
  margin-bottom: 14px;
}
.filter-bar .form-group { margin-bottom: 0; }

.hint-banner {
  background: #FFFBEB; border: 1px solid #FDE68A; color: #92400E; padding: 10px 16px; border-radius: var(--radius);
  font-size: 12px; margin-bottom: 16px; line-height: 1.5;
}

.table-toolbar {
  padding: 12px 20px; background: #FAFAFA; border-bottom: 1px solid var(--border); display: flex;
  justify-content: space-between; align-items: center;
}

.teacher-divider-row td {
  background: #F3F4F6 !important;
  padding: 8px 16px !important;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}

.teacher-divider-content {
  display: flex; justify-content: space-between; align-items: center;
}

.teacher-name-box {
  display: flex; align-items: center; gap: 8px; font-size: 13px;
}

.teacher-avatar {
  width: 22px; height: 22px; border-radius: 50%; background: #E5E7EB; color: #374151; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center; font-size: 11px;
}

.btn-text-link {
  background: none; border: none; color: var(--primary); font-size: 12px; font-weight: 600; cursor: pointer;
  padding: 2px 6px;
}
.btn-text-link:hover { text-decoration: underline; }

.class-item-row td {
  vertical-align: middle;
}
.class-item-row.row-selected {
  background: #FEF2F2 !important;
}

.class-code {
  font-weight: 700; color: var(--danger); font-size: 14px;
}

.reason-input-wrapper {
  position: relative;
}
.reason-input {
  font-size: 12px;
  transition: all 0.15s;
}
.reason-input:focus {
  border-color: var(--danger);
  box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.1);
}
.reason-input.reason-error {
  border-color: var(--danger);
  background: #FFF5F5;
}
.reason-error-hint {
  color: var(--danger); font-size: 10px; position: absolute; right: 8px; top: 8px; pointer-events: none;
}

.sticky-footer {
  position: sticky; bottom: 0; margin-top: 16px; background: #FFF; border: 1px solid var(--border);
  border-radius: var(--radius); padding: 12px 20px; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05); z-index: 5;
}

/* Bước 2 Assign Cards */
.assign-card {
  background: #FFF; border: 1px solid var(--border); border-left: 4px solid var(--border);
  border-radius: var(--radius); margin-bottom: 12px;
}
.assign-card.complete { border-left-color: var(--success); }
.assign-card.invalid { border-left-color: var(--danger); }
.assign-card-head {
  display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; border-bottom: 1px solid var(--border-light, #F3F4F6);
}
.assign-card-body {
  display: grid; grid-template-columns: 1.4fr 1fr 1fr 1fr; gap: 12px; padding: 12px 16px;
}
.slot-label { font-size: 11px; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; text-transform: uppercase; }

.slot-box {
  min-height: 36px; border: 1px dashed var(--border); border-radius: var(--radius); padding: 4px 8px;
  background: #FAFAFA; display: flex; align-items: center; justify-content: space-between;
}
.slot-box.has-val { border-style: solid; border-color: var(--primary); background: #FFF; }
.slot-box.is-invalid { border-color: var(--danger); background: #FEF2F2; }

.assigned-chip {
  display: inline-flex; align-items: center; gap: 6px; background: var(--primary-bg); color: var(--primary);
  padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.chip-remove { background: none; border: none; color: var(--danger); cursor: pointer; font-size: 11px; }

.btn-link {
  background: none; border: none; color: var(--primary); font-size: 12px; font-weight: 600; cursor: pointer;
  padding: 2px 4px;
}
.btn-link:hover { text-decoration: underline; }

@media (max-width: 992px) {
  .filter-bar { grid-template-columns: 1fr 1fr; }
  .assign-card-body { grid-template-columns: 1fr 1fr; }
}
</style>
