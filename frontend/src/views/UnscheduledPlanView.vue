<template>
  <div>
    <!-- Header trang -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
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
      <!-- Bộ lọc mặc định -- Chọn bộ môn -- -->
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
        <div class="form-group">
          <label class="form-label">Tìm nhanh</label>
          <input v-model="searchQuery" class="form-control" placeholder="Tìm giảng viên, mã lớp, môn..." @input="currentPage = 1" />
        </div>
      </div>

      <div class="hint-banner">
        ⚠️ Áp dụng khi có phản ánh hoặc kiểm tra đột xuất. Hiển thị tất cả lớp học trong kỳ &amp; block đã chọn. <strong>Bắt buộc nhập lý do cho từng lớp chọn</strong>.
      </div>

      <!-- Trạng thái tải / Trống -->
      <div v-if="loading" class="card" style="text-align: center; padding: 40px; color: var(--text-muted);">
        Đang tải danh sách lớp học phần...
      </div>

      <div v-else-if="flatClasses.length === 0" class="empty-state card">
        <div class="empty-icon">📂</div>
        <div class="empty-title">Không tìm thấy lớp học phần phù hợp</div>
        <div class="muted" style="font-size: 13px;">Thử đổi từ khóa tìm kiếm hoặc kiểm tra lại lịch dạy đã import.</div>
      </div>

      <!-- Bảng danh sách lớp có phân trang 4 lớp 1 trang -->
      <div v-else class="card" style="padding: 0; overflow: hidden;">
        <div class="table-toolbar">
          <div style="font-size: 13px; font-weight: 600;">
            Danh sách lớp học phần (Tổng {{ flatClasses.length }} lớp)
          </div>
          <div style="display: flex; gap: 8px;">
            <button class="btn btn-secondary" style="font-size: 12px; padding: 5px 12px;" @click="toggleSelectAllPage">
              {{ allPageSelected ? 'Bỏ chọn trang này' : 'Chọn 4 lớp trang này' }}
            </button>
          </div>
        </div>

        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 44px; text-align: center;">Chọn</th>
                <th style="width: 130px;">Mã lớp</th>
                <th>Môn học</th>
                <th>Bộ môn</th>
                <th>Giảng viên</th>
                <th style="width: 140px;">Trạng thái</th>
                <th style="min-width: 280px;">Lý do dự giờ đột xuất <span class="req">*</span></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="cls in pagedClasses"
                :key="cls.teachingId"
                :class="{ 'row-selected': cls.selected }"
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
                  <span class="badge badge-gray">{{ cls.facultyName || 'N/A' }}</span>
                </td>
                <td>
                  <strong>{{ cls.teacherName }}</strong>
                  <div class="muted" style="font-size: 11px;">({{ cls.teacherAccount }})</div>
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
            </tbody>
          </table>
        </div>

        <!-- Phân trang 4 lớp 1 trang -->
        <div v-if="totalPages > 1" class="pagination-bar">
          <div class="pagination-info">
            Trang <strong>{{ currentPage }}</strong> / <strong>{{ totalPages }}</strong> (Tổng <strong>{{ flatClasses.length }}</strong> lớp)
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

      <!-- Thanh cố định chân trang -->
      <div v-if="!loading && flatClasses.length > 0" class="sticky-footer">
        <div>
          Đã chọn <strong style="color: var(--danger); font-size: 15px;">{{ selectedClasses.length }}</strong> lớp đột xuất
          <span v-if="missingReasonCount > 0" class="muted" style="color: var(--danger); margin-left: 8px;">
            (⚠️ còn {{ missingReasonCount }} lớp chưa nhập lý do)
          </span>
        </div>
        <button
          class="btn btn-danger"
          :disabled="selectedClasses.length === 0"
          @click="proceedToAssign"
        >
          Tiếp tục: Phân công người dự ({{ selectedClasses.length }}) →
        </button>
      </div>
    </div>

    <!-- BƯỚC 2: PHÂN CÔNG NGƯỜI DỰ GIỜ -->
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
          <!-- Thẻ lớp phân công dạng Card gọn gàng (phân trang 4 lớp) -->
          <div
            v-for="item in pagedAssignItems"
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
                    <button class="chip-remove" @click="removeSlotInspector(item, 1)">✕</button>
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
                    <button class="chip-remove" @click="removeSlotInspector(item, 2)">✕</button>
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
                    <button class="chip-remove" @click="removeSlotInspector(item, 3)">✕</button>
                  </div>
                  <button v-else class="btn-link" @click="openSearch(item, 3)">+ Tìm gán</button>
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

        <div class="sticky-footer" style="justify-content: flex-end; gap: 12px; margin: 0; border-radius: 0;">
          <button class="btn btn-secondary" :disabled="submitting" @click="step = 1">Xem lại lớp</button>
          <button class="btn btn-secondary" :disabled="submitting" @click="savePlan('DRAFT')">
            {{ submitting ? 'Đang lưu...' : 'Lưu bản nháp' }}
          </button>
          <button class="btn btn-danger" :disabled="submitting" @click="submitPlan">
            {{ submitting ? 'Đang gửi...' : 'Gửi trình duyệt TBĐT →' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal tìm người dự -->
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
import { toast } from '../services/toast.js'
import InspectorSearchModal from '../components/InspectorSearchModal.vue'

const router = useRouter()
const step = ref(1)
const semesters = ref([])
const myFaculties = ref([])

const selectedSemesterId = ref(null)
const selectedBlock = ref(0) // Mặc định: -- Tất cả block --
const selectedFacultyId = ref(null) // Mặc định: -- Chọn bộ môn --
const searchQuery = ref('')

const teacherGroups = ref([])
const loading = ref(false)
const attempted = ref(false)
const submitAttempted = ref(false)
const submitting = ref(false)

// Phân trang 4 lớp 1 trang
const pageSize = 4
const currentPage = ref(1)
const step2Page = ref(1)

const assignItems = ref([])
const modalOpen = ref(false)
const targetItem = ref(null)
const targetSlot = ref(1)

onMounted(async () => {
  try {
    const me = await api.getMe()
    myFaculties.value = me.faculties || []
    selectedSemesterId.value = null // Mặc định: -- Chọn kỳ học --
    await fetchCandidates()
  } catch (e) {
    console.error(e)
  }
})

function onFilterChange() {
  currentPage.value = 1
  fetchCandidates()
}

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
    currentPage.value = 1
  } catch (e) {
    toast.error('Lỗi tải danh sách lớp: ' + e.message)
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

const flatClasses = computed(() => {
  const list = []
  filteredGroups.value.forEach(grp => {
    grp.classes.forEach(c => {
      list.push({
        ...c,
        teacherName: grp.teacherName,
        teacherAccount: grp.teacherAccount,
        teacherId: grp.teacherId
      })
    })
  })
  return list
})

const totalPages = computed(() => Math.ceil(flatClasses.value.length / pageSize) || 1)

const pagedClasses = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return flatClasses.value.slice(start, start + pageSize)
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

const allPageSelected = computed(() => {
  if (pagedClasses.value.length === 0) return false
  return pagedClasses.value.every(c => c.selected)
})

function toggleSelectAllPage() {
  const target = !allPageSelected.value
  pagedClasses.value.forEach(c => {
    c.selected = target
    if (!target) c.reason = ''
  })
}

function onClassToggle(cls) {
  if (!cls.selected) cls.reason = ''
  else toast.info(`Đã chọn lớp ${cls.classCode}`)
}

async function proceedToAssign() {
  attempted.value = true
  if (missingReasonCount.value > 0) {
    const firstMissing = selectedClasses.value.find(c => !c.reason || !c.reason.trim())
    toast.warning(`Lớp ${firstMissing?.classCode || ''} chưa nhập lý do đột xuất!`)
    return
  }

  step.value = 2
  step2Page.value = 1
  const items = []
  for (const c of selectedClasses.value) {
    const sessions = await api.getValidSessions(c.teachingId)
    items.push({
      teachingId: c.teachingId,
      classCode: c.classCode,
      subjectCode: c.subjectCode,
      teacherName: c.teacherName,
      teacherAccount: c.teacherAccount,
      facultyId: c.facultyId,
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

const totalStep2Pages = computed(() => Math.ceil(assignItems.value.length / 4) || 1)
const pagedAssignItems = computed(() => {
  const start = (step2Page.value - 1) * 4
  return assignItems.value.slice(start, start + 4)
})

function onSessionChange(item) {
  const s = item.availableSessions.find(x => x.sessionId === item.selectedSessionId)
  item.plannedDate = s ? s.date : null
  item.shift = s ? s.shift : null
  toast.info(`Đã chọn buổi dự cho lớp ${item.classCode}`)
}

function isItemComplete(item) {
  return !!(item.selectedSessionId && item.inspector1 && item.inspector2)
}

function openSearch(item, slot) {
  targetItem.value = item
  targetSlot.value = slot
  modalOpen.value = true
}

function onInspectorSelected(user) {
  if (targetItem.value) {
    targetItem.value['inspector' + targetSlot.value] = user
    toast.success(`Đã gán GV ${user.account} cho lớp ${targetItem.value.classCode}`)
  }
  modalOpen.value = false
}

function removeSlotInspector(item, slot) {
  const old = item['inspector' + slot]
  item['inspector' + slot] = null
  toast.info(`Đã bỏ phân công GV ${old ? old.account : ''}`)
}

async function savePlan(status) {
  submitting.value = true
  try {
    const payload = {
      facultyId: selectedFacultyId.value || assignItems.value[0]?.facultyId || (myFaculties.value[0]?.id ?? 1),
      semesterId: selectedSemesterId.value,
      block: selectedBlock.value || 1,
      unscheduled: true,
      items: assignItems.value.map(it => ({
        teachingId: it.teachingId,
        plannedDate: it.plannedDate,
        shift: it.shift,
        inspector1Id: it.inspector1?.id ?? null,
        inspector2Id: it.inspector2?.id ?? null,
        inspector3Id: it.inspector3?.id ?? null,
        reason: it.reason
      }))
    }

    if (status === 'DRAFT') {
      await api.savePlanDraft(payload)
      toast.success('Đã lưu bản nháp kế hoạch đột xuất thành công!')
    } else {
      await api.submitPlan(payload, null)
      toast.success('Đã gửi trình duyệt kế hoạch đột xuất thành công!')
    }
    setTimeout(() => {
      router.push('/plans')
    }, 600)
  } catch (e) {
    toast.error('Lỗi: ' + e.message)
  } finally {
    submitting.value = false
  }
}

async function submitPlan() {
  submitAttempted.value = true
  const invalidItem = assignItems.value.find(it => !isItemComplete(it))
  if (invalidItem) {
    toast.warning(`Lớp ${invalidItem.classCode} chưa chọn đủ buổi dự hoặc chưa đủ GV1, GV2!`)
    return
  }
  await savePlan('SUBMITTED')
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
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; padding: 16px 20px; align-items: end;
}

.hint-banner {
  background: #FFF7ED; border: 1px solid #FFEDD5; border-left: 4px solid var(--danger);
  border-radius: var(--radius); padding: 12px 16px; font-size: 13px; color: #9A3412; margin-bottom: 16px;
}

.table-toolbar {
  padding: 12px 20px; background: #FAFAFA; border-bottom: 1px solid var(--border);
  display: flex; justify-content: space-between; align-items: center;
}

.data-table tbody tr { cursor: pointer; }
.row-selected { background: #FEF2F2; }

.class-code { font-weight: 700; color: var(--primary); }

.reason-input-wrapper { display: flex; flex-direction: column; gap: 4px; }
.reason-input { font-size: 12px; padding: 6px 10px; }
.reason-input:disabled { background: #F9FAFB; cursor: not-allowed; }
.reason-error { border-color: var(--danger); background: #FEF2F2; }
.reason-error-hint { font-size: 11px; color: var(--danger); font-weight: 600; }

.sticky-footer {
  position: sticky; bottom: 0; margin-top: 16px; background: #FFF; border: 1px solid var(--border);
  border-radius: var(--radius); padding: 12px 20px; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05); z-index: 5;
}

/* Thẻ phân công Step 2 */
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
  display: grid; grid-template-columns: 1.4fr 1fr 1fr 1fr; gap: 12px; padding: 12px 16px; align-items: start;
}

.slot-label { font-size: 11px; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; text-transform: uppercase; }

.slot-box {
  min-height: 38px; border: 1px dashed var(--border); border-radius: var(--radius);
  padding: 4px; background: #FAFAFA; display: flex; align-items: center; justify-content: center;
}
.slot-box.has-val { border-style: solid; border-color: var(--primary); background: #FFF; }
.slot-box.is-invalid { border-color: var(--danger); background: #FEF2F2; }

.assigned-chip {
  display: inline-flex; align-items: center; justify-content: space-between; width: 100%;
  background: var(--primary-bg); color: var(--primary); padding: 6px 10px; border-radius: 8px; font-size: 12px; font-weight: 600;
}
.chip-remove { background: none; border: none; color: var(--danger); cursor: pointer; font-size: 12px; font-weight: bold; padding: 0 4px; }

.btn-link {
  background: none; border: none; color: var(--primary); font-size: 12px; font-weight: 600; cursor: pointer;
}
</style>
