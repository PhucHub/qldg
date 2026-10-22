<template>
  <div>
    <!-- Header trang -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <div style="display: flex; align-items: center; gap: 10px;">
          <h1 class="page-title" style="color: var(--danger);">
            {{ editingPlanId ? `Chỉnh sửa kế hoạch đột xuất (${editingPlanCode})` : 'Lập kế hoạch dự giờ đột xuất' }}
          </h1>
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

    <!-- BƯỚC 1: CHỌN LỚP & NHẬP LÝ DO (GOM NHÓM THEO GIẢNG VIÊN - UC-13) -->
    <div v-if="step === 1">
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
        ⚠️ Áp dụng khi có phản ánh hoặc kiểm tra đột xuất. Hiển thị tất cả lớp học trong kỳ &amp; block đã chọn gom theo giảng viên. <strong>Bắt buộc nhập lý do cho từng lớp chọn</strong>.
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

      <!-- Danh sách lớp gom nhóm theo từng Giảng viên (SRS UC-13 & HDSD 4.2 & Test Case 7.2) -->
      <div v-else>
        <div
          v-for="grp in pagedGroups"
          :key="grp.teacherAccount"
          class="card teacher-group-card"
          style="padding: 0; overflow: hidden; margin-bottom: 16px;"
        >
          <!-- Header Giảng viên -->
          <div class="teacher-group-header">
            <div style="display: flex; align-items: center; gap: 12px;">
              <div class="teacher-avatar">
                {{ (grp.teacherName || grp.teacherAccount || 'G').charAt(0).toUpperCase() }}
              </div>
              <div>
                <div style="font-weight: 700; font-size: 14px; color: var(--text-main);">
                  {{ grp.teacherName }}
                  <span class="muted" style="font-weight: 500;">({{ grp.teacherAccount }})</span>
                </div>
                <div style="font-size: 12px; color: var(--text-muted);">
                  Bộ môn: <strong>{{ grp.facultyName || 'Chuyên môn' }}</strong> · Đang dạy <strong>{{ grp.classes.length }}</strong> lớp học phần
                </div>
              </div>
            </div>
            <div>
              <button
                class="btn btn-secondary"
                style="font-size: 12px; padding: 5px 12px;"
                @click="toggleSelectTeacherGroup(grp)"
              >
                {{ isAllGroupSelected(grp) ? `Bỏ chọn ${grp.classes.length} lớp` : `+ Chọn cả ${grp.classes.length} lớp` }}
              </button>
            </div>
          </div>

          <!-- Bảng lớp của Giảng viên -->
          <div class="table-responsive">
            <table class="data-table">
              <thead>
                <tr>
                  <th style="width: 44px; text-align: center;">Chọn</th>
                  <th style="width: 130px;">Mã lớp</th>
                  <th>Môn học</th>
                  <th>Bộ môn</th>
                  <th style="width: 160px;">Trạng thái</th>
                  <th style="min-width: 300px;">Lý do dự giờ đột xuất <span class="req">*</span></th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="cls in grp.classes"
                  :key="cls.teachingId"
                  :class="{ 'row-selected': selectedTeachingIds.includes(cls.teachingId) }"
                  @click="toggleSelectClass(cls, !selectedTeachingIds.includes(cls.teachingId))"
                >
                  <td style="text-align: center;" @click.stop>
                    <input
                      type="checkbox"
                      :checked="selectedTeachingIds.includes(cls.teachingId)"
                      @change="toggleSelectClass(cls, $event.target.checked)"
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
                    <span class="badge badge-gray">{{ cls.facultyName || grp.facultyName || 'N/A' }}</span>
                  </td>
                  <td>
                    <span v-if="cls.alreadyPlanned" class="badge badge-gray" :title="'Đã thuộc kế hoạch ' + cls.existingPlanCode">
                      Đã có KH ({{ cls.existingPlanCode }})
                    </span>
                    <span v-else class="badge badge-blue">Chưa có KH</span>
                  </td>
                  <td @click.stop>
                    <div class="reason-input-wrapper">
                      <input
                        v-model="reasonsMap[cls.teachingId]"
                        class="form-control reason-input"
                        :class="{ 'reason-error': attempted && selectedTeachingIds.includes(cls.teachingId) && !getReason(cls.teachingId) }"
                        :placeholder="selectedTeachingIds.includes(cls.teachingId) ? 'Ghi rõ lý do (VD: Sinh viên phản ánh giảng dạy, KT đột xuất)...' : 'Tích chọn để nhập lý do'"
                        :disabled="!selectedTeachingIds.includes(cls.teachingId)"
                        @focus="onReasonFocus(cls)"
                      />
                      <span v-if="attempted && selectedTeachingIds.includes(cls.teachingId) && !getReason(cls.teachingId)" class="reason-error-hint">
                        Bắt buộc nhập lý do
                      </span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Phân trang theo nhóm giảng viên -->
        <div v-if="totalPages > 1" class="card pagination-bar" style="margin-bottom: 16px;">
          <div class="pagination-info">
            Trang <strong>{{ currentPage }}</strong> / <strong>{{ totalPages }}</strong> (Tổng <strong>{{ filteredGroups.length }}</strong> giảng viên - <strong>{{ flatClasses.length }}</strong> lớp)
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

    <!-- BƯỚC 2: PHÂN CÔNG NGƯỜI DỰ GIỜ (KÉO THẢ / CHỌN GV + TÌM CHÉO - UC-11, UC-12, UC-13) -->
    <div v-if="step === 2">
      <div class="step2-layout">
        <!-- Cột trái: Danh sách giảng viên trong bộ môn để kéo thả -->
        <div class="card inspector-sidebar">
          <div style="font-weight: 700; font-size: 13px; margin-bottom: 4px; color: var(--text-main);">
            Giảng viên trong bộ môn
          </div>
          <div class="muted" style="font-size: 11px; margin-bottom: 10px;">
            Kéo thả giảng viên vào ô GV 1 / GV 2 / GV 3 bên phải hoặc chọn trực tiếp
          </div>
          <input
            v-model="inspectorFilter"
            class="form-control"
            style="font-size: 12px; margin-bottom: 10px;"
            placeholder="Lọc tên / tài khoản GV..."
          />
          <div class="inspector-list">
            <div
              v-for="ins in filteredFacultyInspectors"
              :key="ins.id || ins.account"
              class="inspector-chip-draggable"
              draggable="true"
              @dragstart="onDragStart($event, ins)"
            >
              <div>
                <div style="font-weight: 600; font-size: 12px;">{{ ins.fullName }}</div>
                <div class="muted" style="font-size: 11px;">{{ ins.account }} · {{ ins.facultyName }}</div>
              </div>
              <span class="drag-handle" title="Kéo thả vào lớp">⠿</span>
            </div>
            <div v-if="filteredFacultyInspectors.length === 0" class="muted" style="font-size: 12px; text-align: center; padding: 12px;">
              Không có giảng viên phù hợp
            </div>
          </div>
        </div>

        <!-- Cột phải: Danh sách lớp cần phân công -->
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
                  <div
                    class="slot-box"
                    :class="{ 'has-val': item.inspector1, 'is-invalid': submitAttempted && !item.inspector1 }"
                    @dragover.prevent
                    @drop="onDropInspector($event, item, 1)"
                  >
                    <div v-if="item.inspector1" class="assigned-chip">
                      <span>{{ item.inspector1.account }} ({{ item.inspector1.fullName }})</span>
                      <button class="chip-remove" @click="removeSlotInspector(item, 1)">✕</button>
                    </div>
                    <div v-else class="slot-picker-row">
                      <select
                        class="form-control slot-select"
                        :value="''"
                        @change="onSelectDropdownInspector(item, 1, $event.target.value); $event.target.value = ''"
                      >
                        <option value="">-- Kéo thả / Chọn GV 1 --</option>
                        <option
                          v-for="ins in getEligibleFacultyInspectors(item, 1)"
                          :key="ins.account"
                          :value="ins.account"
                        >
                          {{ ins.account }} - {{ ins.fullName }}
                        </option>
                      </select>
                      <button
                        type="button"
                        class="btn-cross-plus"
                        title="Tìm giảng viên bộ môn khác / cơ sở khác (Dự giờ chéo)"
                        @click="openSearch(item, 1)"
                      >
                        +
                      </button>
                    </div>
                  </div>
                </div>

                <!-- GV 2 -->
                <div>
                  <div class="slot-label">GV 2 <span class="req">*</span></div>
                  <div
                    class="slot-box"
                    :class="{ 'has-val': item.inspector2, 'is-invalid': submitAttempted && !item.inspector2 }"
                    @dragover.prevent
                    @drop="onDropInspector($event, item, 2)"
                  >
                    <div v-if="item.inspector2" class="assigned-chip">
                      <span>{{ item.inspector2.account }} ({{ item.inspector2.fullName }})</span>
                      <button class="chip-remove" @click="removeSlotInspector(item, 2)">✕</button>
                    </div>
                    <div v-else class="slot-picker-row">
                      <select
                        class="form-control slot-select"
                        :value="''"
                        @change="onSelectDropdownInspector(item, 2, $event.target.value); $event.target.value = ''"
                      >
                        <option value="">-- Kéo thả / Chọn GV 2 --</option>
                        <option
                          v-for="ins in getEligibleFacultyInspectors(item, 2)"
                          :key="ins.account"
                          :value="ins.account"
                        >
                          {{ ins.account }} - {{ ins.fullName }}
                        </option>
                      </select>
                      <button
                        type="button"
                        class="btn-cross-plus"
                        title="Tìm giảng viên bộ môn khác / cơ sở khác (Dự giờ chéo)"
                        @click="openSearch(item, 2)"
                      >
                        +
                      </button>
                    </div>
                  </div>
                </div>

                <!-- GV 3 -->
                <div>
                  <div class="slot-label">GV 3 <span class="muted">(tùy chọn)</span></div>
                  <div
                    class="slot-box"
                    :class="{ 'has-val': item.inspector3 }"
                    @dragover.prevent
                    @drop="onDropInspector($event, item, 3)"
                  >
                    <div v-if="item.inspector3" class="assigned-chip">
                      <span>{{ item.inspector3.account }} ({{ item.inspector3.fullName }})</span>
                      <button class="chip-remove" @click="removeSlotInspector(item, 3)">✕</button>
                    </div>
                    <div v-else class="slot-picker-row">
                      <select
                        class="form-control slot-select"
                        :value="''"
                        @change="onSelectDropdownInspector(item, 3, $event.target.value); $event.target.value = ''"
                      >
                        <option value="">-- Kéo thả / Chọn GV 3 --</option>
                        <option
                          v-for="ins in getEligibleFacultyInspectors(item, 3)"
                          :key="ins.account"
                          :value="ins.account"
                        >
                          {{ ins.account }} - {{ ins.fullName }}
                        </option>
                      </select>
                      <button
                        type="button"
                        class="btn-cross-plus"
                        title="Tìm giảng viên bộ môn khác / cơ sở khác (Dự giờ chéo)"
                        @click="openSearch(item, 3)"
                      >
                        +
                      </button>
                    </div>
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
    </div>

    <!-- Modal tìm người dự chéo -->
    <InspectorSearchModal
      :is-open="modalOpen"
      :exclude-accounts="currentExcludeAccounts"
      @close="modalOpen = false"
      @select="onInspectorSelected"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'
import InspectorSearchModal from '../components/InspectorSearchModal.vue'

const router = useRouter()
const route = useRoute()
const step = ref(1)
const semesters = ref([])
const myFaculties = ref([])

const selectedSemesterId = ref(null)
const selectedBlock = ref(0)
const selectedFacultyId = ref(null)
const searchQuery = ref('')

const editingPlanId = ref(null)
const editingPlanCode = ref('')
const existingPlanItemsMap = ref({})

const teacherGroups = ref([])
const loading = ref(false)
const attempted = ref(false)
const submitAttempted = ref(false)
const submitting = ref(false)

const pageSize = 4
const currentPage = ref(1)
const step2Page = ref(1)

const selectedTeachingIds = ref([])
const reasonsMap = ref({})

const assignItems = ref([])
const facultyInspectors = ref([])
const inspectorFilter = ref('')
const modalOpen = ref(false)
const currentUser = ref({})
const targetItem = ref(null)
const targetSlot = ref(1)

onMounted(async () => {
  try {
    const me = await api.getMe()
    currentUser.value = me
    if (me.role === 'TCQLDT' || me.role === 'GV') {
      toast.error('Tài khoản của bạn không có quyền lập kế hoạch dự giờ!')
      router.push('/plans')
      return
    }
    myFaculties.value = me.faculties || []
    if (me.role === 'TM' && myFaculties.value.length === 1) {
      selectedFacultyId.value = myFaculties.value[0].id
    }
    semesters.value = await api.getSemesters()
    if (semesters.value && semesters.value.length > 0) {
      selectedSemesterId.value = semesters.value[0].id
    }

    if (route.query.editId) {
      await loadEditingPlan(route.query.editId)
    } else {
      await fetchCandidates()
    }
  } catch (e) {
    console.error(e)
  }
})

async function loadEditingPlan(planId) {
  try {
    const plan = await api.getPlanDetail(planId)
    if (plan.status !== 'DRAFT' && plan.status !== 'REJECTED_NEED_EDIT') {
      toast.error('Chỉ được phép chỉnh sửa kế hoạch ở trạng thái Bản nháp hoặc Yêu cầu sửa!')
      router.push(`/plans/${planId}`)
      return
    }
    editingPlanId.value = plan.id
    editingPlanCode.value = plan.code
    selectedSemesterId.value = plan.semesterId
    selectedBlock.value = plan.block || 1
    selectedFacultyId.value = plan.facultyId

    await fetchCandidates()

    const ids = []
    const rMap = {}
    const itemMap = {}
    for (const it of (plan.items || [])) {
      if (it.teachingId) {
        ids.push(it.teachingId)
        rMap[it.teachingId] = it.reason || 'Dự giờ đột xuất'
        itemMap[it.teachingId] = it
      }
    }
    selectedTeachingIds.value = ids
    reasonsMap.value = rMap
    existingPlanItemsMap.value = itemMap
  } catch (e) {
    toast.error('Không thể tải kế hoạch cần sửa: ' + e.message)
    await fetchCandidates()
  }
}

function getReason(id) {
  return (reasonsMap.value[id] || '').trim()
}

function onFilterChange() {
  currentPage.value = 1
  selectedTeachingIds.value = []
  reasonsMap.value = {}
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
    teacherGroups.value = res || []
    if (!editingPlanId.value) {
      selectedTeachingIds.value = []
      reasonsMap.value = {}
    }
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
  teacherGroups.value.forEach(grp => {
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

const totalPages = computed(() => Math.ceil(filteredGroups.value.length / pageSize) || 1)

const pagedGroups = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredGroups.value.slice(start, start + pageSize)
})

const selectedClasses = computed(() => {
  return flatClasses.value
    .filter(c => selectedTeachingIds.value.includes(c.teachingId))
    .map(c => ({
      ...c,
      reason: getReason(c.teachingId)
    }))
})

const missingReasonCount = computed(() => {
  return selectedClasses.value.filter(c => !c.reason).length
})

function isAllGroupSelected(grp) {
  if (!grp.classes || grp.classes.length === 0) return false
  return grp.classes.every(c => selectedTeachingIds.value.includes(c.teachingId))
}

function toggleSelectTeacherGroup(grp) {
  const target = !isAllGroupSelected(grp)
  const set = new Set(selectedTeachingIds.value)
  grp.classes.forEach(c => {
    if (target) {
      set.add(c.teachingId)
    } else {
      set.delete(c.teachingId)
      delete reasonsMap.value[c.teachingId]
    }
  })
  selectedTeachingIds.value = Array.from(set)
  if (target) {
    toast.info(`Đã chọn cả ${grp.classes.length} lớp của GV ${grp.teacherName}`)
  }
}

function toggleSelectClass(cls, checked) {
  const i = selectedTeachingIds.value.indexOf(cls.teachingId)
  if (checked) {
    if (i < 0) {
      selectedTeachingIds.value.push(cls.teachingId)
      toast.info(`Đã chọn lớp ${cls.classCode}`)
    }
  } else {
    if (i >= 0) {
      selectedTeachingIds.value.splice(i, 1)
      delete reasonsMap.value[cls.teachingId]
    }
  }
}

function onReasonFocus(cls) {
  if (!selectedTeachingIds.value.includes(cls.teachingId)) {
    toggleSelectClass(cls, true)
  }
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

  try {
    const facId = selectedFacultyId.value || selectedClasses.value[0]?.facultyId || (myFaculties.value[0]?.id ?? 1)
    facultyInspectors.value = await api.getInspectorsInFaculty(facId)
  } catch {
    facultyInspectors.value = []
  }

  const items = []
  for (const c of selectedClasses.value) {
    const sessions = await api.getValidSessions(c.teachingId)
    const prev = existingPlanItemsMap.value[c.teachingId]
    const matchedSession = prev
      ? sessions.find(s => s.date === prev.plannedDate && Number(s.shift) === Number(prev.shift))
      : null
    const chosenSession = matchedSession || (sessions.length > 0 ? sessions[0] : null)

    items.push({
      teachingId: c.teachingId,
      classCode: c.classCode,
      subjectCode: c.subjectCode,
      subjectName: c.subjectName,
      teacherName: c.teacherName,
      teacherAccount: c.teacherAccount,
      facultyId: c.facultyId,
      reason: c.reason.trim(),
      availableSessions: sessions,
      selectedSessionId: chosenSession ? chosenSession.sessionId : null,
      plannedDate: chosenSession ? chosenSession.date : (prev?.plannedDate || null),
      shift: chosenSession ? chosenSession.shift : (prev?.shift || null),
      inspector1: prev?.inspector1 || null,
      inspector2: prev?.inspector2 || null,
      inspector3: prev?.inspector3 || null
    })
  }
  assignItems.value = items
}

const filteredFacultyInspectors = computed(() => {
  const q = inspectorFilter.value.trim().toLowerCase()
  if (!q) return facultyInspectors.value
  return facultyInspectors.value.filter(ins =>
    (ins.account && ins.account.toLowerCase().includes(q)) ||
    (ins.fullName && ins.fullName.toLowerCase().includes(q))
  )
})

function getEligibleFacultyInspectors(item, slot) {
  return facultyInspectors.value.filter(ins => {
    if (item.teacherAccount && ins.account === item.teacherAccount) return false
    for (const s of [1, 2, 3]) {
      if (s !== slot && item['inspector' + s]?.account === ins.account) return false
    }
    return true
  })
}

function assignInspectorToSlot(item, slot, user) {
  if (!item || !user) return
  if (item.teacherAccount && user.account === item.teacherAccount) {
    toast.error(`Giảng viên ${user.account} (${user.fullName}) đứng lớp ${item.classCode}, không thể tự dự giờ lớp của chính mình!`)
    return false
  }
  const duplicateSlot = [1, 2, 3].find(s => s !== slot && item['inspector' + s]?.account === user.account)
  if (duplicateSlot) {
    toast.error(`Giảng viên ${user.account} (${user.fullName}) đã được phân công ở vị trí GV ${duplicateSlot} của lớp ${item.classCode}! Không được phân công trùng!`)
    return false
  }
  item['inspector' + slot] = user
  toast.success(`Đã gán GV ${user.account} vào vị trí GV ${slot} lớp ${item.classCode}`)
  return true
}

function onSelectDropdownInspector(item, slot, account) {
  if (!account) return
  const found = facultyInspectors.value.find(x => x.account === account)
  if (found) {
    assignInspectorToSlot(item, slot, found)
  }
}

function onDragStart(event, inspector) {
  event.dataTransfer.setData('application/json', JSON.stringify(inspector))
}

function onDropInspector(event, item, slot) {
  try {
    const raw = event.dataTransfer.getData('application/json')
    if (!raw) return
    const inspector = JSON.parse(raw)
    assignInspectorToSlot(item, slot, inspector)
  } catch (e) {
    console.error(e)
  }
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

const currentExcludeAccounts = computed(() => {
  if (!targetItem.value) return []
  const accs = []
  if (targetItem.value.teacherAccount) accs.push(targetItem.value.teacherAccount)
  for (const s of [1, 2, 3]) {
    if (s !== targetSlot.value && targetItem.value['inspector' + s]?.account) {
      accs.push(targetItem.value['inspector' + s].account)
    }
  }
  return accs
})

function openSearch(item, slot) {
  targetItem.value = item
  targetSlot.value = slot
  modalOpen.value = true
}

function onInspectorSelected(user) {
  if (!targetItem.value || !user) return
  if (assignInspectorToSlot(targetItem.value, targetSlot.value, user)) {
    modalOpen.value = false
  }
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
        classCode: it.classCode,
        subjectCode: it.subjectCode,
        subjectName: it.subjectName,
        teacherAccount: it.teacherAccount,
        teacherName: it.teacherName,
        plannedDate: it.plannedDate,
        shift: it.shift,
        inspector1Id: it.inspector1?.id ?? null,
        inspector2Id: it.inspector2?.id ?? null,
        inspector3Id: it.inspector3?.id ?? null,
        inspector1: it.inspector1,
        inspector2: it.inspector2,
        inspector3: it.inspector3,
        reason: it.reason
      }))
    }

    if (status === 'DRAFT') {
      await api.savePlanDraft(payload, editingPlanId.value)
      toast.success('Đã lưu bản nháp kế hoạch đột xuất thành công!')
    } else {
      await api.submitPlan(payload, editingPlanId.value)
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

.teacher-group-header {
  padding: 12px 20px;
  background: #FFF5F5;
  border-bottom: 1px solid #FED7D7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.teacher-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--danger);
  color: #FFF;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
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

/* Step 2 Layout */
.step2-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: start;
}

.inspector-sidebar {
  padding: 14px;
  position: sticky;
  top: 16px;
}

.inspector-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 460px;
  overflow-y: auto;
}

.inspector-chip-draggable {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #FAFAFA;
  cursor: grab;
  transition: all 0.15s;
}
.inspector-chip-draggable:hover {
  border-color: var(--primary);
  background: var(--primary-bg);
}
.drag-handle {
  color: var(--text-muted);
  font-size: 14px;
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
  display: grid; grid-template-columns: 1.3fr 1fr 1fr 1fr; gap: 12px; padding: 12px 16px; align-items: start;
}

.slot-label { font-size: 11px; font-weight: 600; color: var(--text-muted); margin-bottom: 4px; text-transform: uppercase; }

.slot-box {
  min-height: 38px; border: 1px dashed var(--border); border-radius: var(--radius);
  padding: 4px; background: #FAFAFA; display: flex; align-items: center; justify-content: center;
}
.slot-box.has-val { border-style: solid; border-color: var(--primary); background: #FFF; }
.slot-box.is-invalid { border-color: var(--danger); background: #FEF2F2; }

.slot-picker-row {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}
.slot-select {
  font-size: 11px;
  padding: 4px 6px;
  flex: 1;
  min-width: 0;
}
.btn-cross-plus {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--primary);
  background: var(--primary-bg);
  color: var(--primary);
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.btn-cross-plus:hover {
  background: var(--primary);
  color: #FFF;
}

.assigned-chip {
  display: inline-flex; align-items: center; justify-content: space-between; width: 100%;
  background: var(--primary-bg); color: var(--primary); padding: 6px 10px; border-radius: 8px; font-size: 12px; font-weight: 600;
}
.chip-remove { background: none; border: none; color: var(--danger); cursor: pointer; font-size: 12px; font-weight: bold; padding: 0 4px; }
</style>
