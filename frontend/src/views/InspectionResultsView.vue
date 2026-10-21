<template>
  <div>
    <!-- Header -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <h1 class="page-title">Kết quả dự giờ & Giám sát tiến độ</h1>
      </div>
      <div>
        <button class="btn btn-secondary" @click="fetchData">
          Làm mới
        </button>
      </div>
    </div>

    <!-- Cảnh báo vi phạm SLA 72h của toàn đơn vị -->
    <div v-if="overdueCount > 0" class="banner-warning" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <div style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 18px;">⚠️</span>
        <div>
          <strong>Cảnh báo vi phạm SLA 72 giờ:</strong> Có <strong>{{ overdueCount }}</strong> buổi dự giờ đã kết thúc quá hạn 72h nhưng giảng viên chưa nộp đủ phiếu đánh giá.
        </div>
      </div>
      <button class="btn btn-danger" style="padding: 4px 12px; font-size: 12px;" @click="filterOverdue">
        Lọc buổi trễ hạn ({{ overdueCount }})
      </button>
    </div>

    <!-- Thống kê nhanh (Quick Stats Cards) -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 20px;">
      <div class="card" style="padding: 16px; margin-bottom: 0;">
        <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted); font-weight: 600;">Tổng số buổi dự giờ</div>
        <div style="font-size: 28px; font-weight: 700; margin-top: 4px; color: var(--text-main);">{{ totalSessions }}</div>
      </div>
      <div class="card" style="padding: 16px; margin-bottom: 0;">
        <div style="font-size: 11px; text-transform: uppercase; color: var(--success); font-weight: 600;">Đã nộp đủ phiếu</div>
        <div style="font-size: 28px; font-weight: 700; margin-top: 4px; color: var(--success);">{{ completedCount }}</div>
      </div>
      <div class="card" style="padding: 16px; margin-bottom: 0;">
        <div style="font-size: 11px; text-transform: uppercase; color: var(--primary); font-weight: 600;">Đang chờ phiếu</div>
        <div style="font-size: 28px; font-weight: 700; margin-top: 4px; color: var(--primary);">{{ pendingCount }}</div>
      </div>
      <div class="card" style="padding: 16px; margin-bottom: 0;">
        <div style="font-size: 11px; text-transform: uppercase; color: var(--danger); font-weight: 600;">Quá hạn SLA 72h</div>
        <div style="font-size: 28px; font-weight: 700; margin-top: 4px; color: var(--danger);">{{ overdueCount }}</div>
      </div>
    </div>

    <!-- Khối bộ lọc (Card) -->
    <div class="card" style="padding: 16px 20px;">
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; align-items: flex-end;">
        <div class="form-group">
          <label class="form-label">Học kỳ</label>
          <select v-model="filters.semesterId" class="form-control" @change="fetchData">
            <option value="">-- Tất cả học kỳ --</option>
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Block</label>
          <select v-model="filters.block" class="form-control" @change="fetchData">
            <option value="">-- Tất cả block --</option>
            <option value="1">Block 1</option>
            <option value="2">Block 2</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Bộ môn quản lý</label>
          <select
            v-model="filters.facultyId"
            class="form-control"
            :disabled="allowedFaculties.length === 1"
            @change="fetchData"
          >
            <option v-if="allowedFaculties.length > 1" value="">-- Tất cả bộ môn --</option>
            <option v-for="f in allowedFaculties" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Trạng thái</label>
          <select v-model="filters.statusFilter" class="form-control" @change="fetchData">
            <option value="ALL">-- Tất cả trạng thái --</option>
            <option value="COMPLETED">Đã hoàn thành (đủ phiếu)</option>
            <option value="PENDING">Đang chờ phiếu</option>
            <option value="OVERDUE">Quá hạn 72h</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Tìm kiếm</label>
          <input
            v-model="filters.search"
            class="form-control"
            placeholder="Giảng viên, mã lớp, môn, kế hoạch..."
            @input="debounceFetch"
          />
        </div>
      </div>
    </div>

    <!-- Bảng danh sách kết quả dự giờ (Card) -->
    <div v-if="loading" class="card" style="text-align: center; padding: 40px; color: var(--text-muted);">
      Đang tải danh sách kết quả dự giờ...
    </div>

    <div v-else-if="sessions.length === 0" class="empty-state card">
      <div class="empty-icon">📊</div>
      <div class="empty-title">Không tìm thấy kết quả dự giờ</div>
      <div style="font-size: 13px;">Không có buổi dự giờ nào phù hợp với bộ lọc hiện tại.</div>
    </div>

    <div v-else class="card" style="padding: 0; overflow: hidden;">
      <div style="padding: 14px 20px; background: #FFF; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between;">
        <h2 class="card-title" style="margin-bottom: 0;">
          Danh sách buổi dự giờ & Kết quả đánh giá
        </h2>
        <span class="badge badge-gray">{{ sessions.length }} buổi</span>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th>Mã kế hoạch</th>
              <th>Giảng viên được dự</th>
              <th>Môn học & Lớp</th>
              <th>Thời gian & Phòng</th>
              <th>Người dự giờ phân công</th>
              <th style="text-align: center;">Kết quả chấm</th>
              <th style="text-align: center;">Trạng thái</th>
              <th style="text-align: center;">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(s, idx) in sessions"
              :key="s.id"
              :style="{ background: s.isOverdue ? 'var(--danger-bg)' : undefined }"
            >
              <td class="text-center font-mono">{{ idx + 1 }}</td>
              <td style="font-weight: 600;">
                <router-link :to="`/plans/${s.planId}`" class="badge badge-blue" style="text-decoration: none;" :title="`Xem chi tiết kế hoạch ${s.planCode}`">
                  {{ s.planCode }}
                </router-link>
              </td>
              <td>
                <div style="font-weight: 600; color: var(--text-main);">{{ s.teacherName }}</div>
                <div style="font-size: 12px; color: var(--text-muted);">({{ s.teacherAccount }}) &bull; {{ s.facultyName }}</div>
              </td>
              <td>
                <div style="font-weight: 600; color: var(--text-main); margin-bottom: 4px;">{{ s.subjectName }}</div>
                <div style="display: flex; gap: 6px; font-size: 11px;">
                  <span class="badge badge-blue">{{ s.subjectCode }}</span>
                  <span class="badge badge-gray">{{ s.classCode }}</span>
                </div>
              </td>
              <td>
                <div style="font-weight: 600;">{{ formatDate(s.plannedDate) }}</div>
                <div style="font-size: 12px; color: var(--text-muted);">Ca {{ s.shift }} &bull; P.{{ s.room || 'F201' }}</div>
              </td>
              <td>
                <div style="display: flex; flex-direction: column; gap: 4px;">
                  <div
                    v-for="ins in s.assignedInspectors"
                    :key="ins.account"
                    style="font-size: 12px; display: flex; align-items: center; gap: 6px;"
                  >
                    <span style="font-weight: 600; color: var(--text-muted); font-size: 11px;">{{ ins.roleLabel }}:</span>
                    <span>{{ ins.fullName }}</span>
                    <span :style="{ color: ins.isSubmitted ? 'var(--success)' : 'var(--text-muted)', fontWeight: ins.isSubmitted ? '600' : '400', fontSize: '11px' }">
                      {{ ins.isSubmitted ? `(✓ ${ins.evaluation?.score}đ)` : '(chưa nộp)' }}
                    </span>
                  </div>
                </div>
              </td>
              <td style="text-align: center;">
                <template v-if="s.evaluations && s.evaluations.length > 0">
                  <div style="font-weight: 700; font-size: 14px;">{{ s.avgScore }}/92đ</div>
                  <span
                    class="badge"
                    :class="s.avgPercent >= 75 ? 'badge-green' : 'badge-red'"
                    style="margin-top: 2px;"
                  >
                    {{ s.avgPercent >= 75 ? 'ĐẠT (' + s.avgPercent + '%)' : 'CHƯA ĐẠT (' + s.avgPercent + '%)' }}
                  </span>
                </template>
                <template v-else>
                  <span class="badge badge-gray">Chưa có phiếu</span>
                </template>
              </td>
              <td style="text-align: center;">
                <span v-if="s.isOverdue" class="badge badge-red">
                  Quá hạn 72h
                </span>
                <span v-else-if="s.isCompleted" class="badge badge-green">
                  Hoàn thành
                </span>
                <span v-else class="badge badge-gray">
                  {{ s.submittedCount }}/{{ s.totalAssigned }} phiếu
                </span>
              </td>
              <td style="text-align: center; white-space: nowrap;">
                <!-- Nút Nhập phiếu / Sửa phiếu của tôi nếu tài khoản hiện tại được phân công đi dự giờ -->
                <template v-if="getMyInspectorAssignment(s)">
                  <button
                    v-if="!getMyInspectorAssignment(s).isSubmitted"
                    class="btn btn-primary"
                    style="padding: 4px 10px; font-size: 12px; margin-right: 6px;"
                    title="Nhập phiếu đánh giá 23 tiêu chí cho tiết dạy này"
                    @click="openEvaluationModal(s, getMyInspectorAssignment(s))"
                  >
                    📝 Nhập phiếu của tôi
                  </button>
                  <button
                    v-else
                    class="btn btn-outline-primary"
                    style="padding: 4px 8px; font-size: 12px; margin-right: 6px;"
                    title="Sửa lại phiếu đánh giá của tôi"
                    @click="openEvaluationModal(s, getMyInspectorAssignment(s))"
                  >
                    ✏️ Sửa phiếu của tôi
                  </button>
                </template>

                <button
                  class="btn btn-secondary"
                  style="padding: 4px 10px; font-size: 12px;"
                  :disabled="!s.evaluations || s.evaluations.length === 0"
                  :title="s.evaluations && s.evaluations.length > 0 ? 'Xem chi tiết phiếu đánh giá' : 'Chưa có phiếu nào được nộp'"
                  @click="openViewModal(s)"
                >
                  Xem phiếu
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL XEM PHIẾU ĐÁNH GIÁ (UC-17) -->
    <div v-if="showViewModal" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content" style="max-width: 900px;">
        <div class="modal-header">
          <div>
            <div class="modal-title">Chi tiết phiếu đánh giá dự giờ (UC-17)</div>
            <div style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">
              Buổi dự: <strong>{{ activeSession?.classCode }} - {{ activeSession?.subjectName }}</strong> &bull;
              GV dạy: <strong>{{ activeSession?.teacherName }} ({{ activeSession?.teacherAccount }})</strong>
            </div>
          </div>
          <button class="btn-icon" @click="closeViewModal">✕</button>
        </div>

        <div class="modal-body printable-area">
          <div v-if="loadingView" style="text-align: center; padding: 40px; color: var(--text-muted);">
            Đang tải phiếu đánh giá...
          </div>
          <div v-else-if="sessionEvaluations.length === 0" class="empty-state" style="padding: 24px;">
            <p>Chưa có phiếu đánh giá nào được nộp cho buổi dự giờ này.</p>
          </div>
          <div v-else>
            <!-- Inspector selector tabs -->
            <div class="tabs-nav" style="margin-bottom: 14px;">
              <button
                v-for="(ev, index) in sessionEvaluations"
                :key="ev.id"
                class="tab-btn"
                :class="{ active: selectedEvalIndex === index }"
                @click="selectedEvalIndex = index"
              >
                Phiếu {{ index + 1 }}: {{ ev.inspectorName }} ({{ ev.score }}/92đ)
              </button>
            </div>

            <!-- Current Evaluation Summary -->
            <div v-if="currentSelectedEval">
              <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; padding: 12px; background: var(--gray-bg); border-radius: var(--radius); font-size: 13px; margin-bottom: 14px;">
                <div>Người đánh giá: <strong>{{ currentSelectedEval.inspectorName }} ({{ currentSelectedEval.inspectorAccount }})</strong></div>
                <div>Thời gian gửi: <strong>{{ formatDateTime(currentSelectedEval.submittedAt) }}</strong></div>
                <div>Tổng điểm: <strong>{{ currentSelectedEval.score }} / 92 điểm</strong></div>
                <div>
                  Kết quả:
                  <span
                    class="badge"
                    :class="currentSelectedEval.percentage >= 75 ? 'badge-green' : 'badge-red'"
                  >
                    {{ currentSelectedEval.percentage }}% &bull; {{ currentSelectedEval.percentage >= 75 ? 'ĐẠT YÊU CẦU' : 'KHÔNG ĐẠT' }}
                  </span>
                </div>
              </div>

              <!-- Nhận xét của người dự -->
              <div class="card" style="padding: 12px 16px; margin-bottom: 14px; background: #FFF;">
                <div style="font-weight: 600; font-size: 13px; margin-bottom: 6px;">Nhận xét & đánh giá chung:</div>
                <div style="font-size: 13px; color: var(--text-main); line-height: 1.5;">{{ currentSelectedEval.note || 'Không có nhận xét thêm.' }}</div>
              </div>

              <!-- Chi tiết điểm 23 câu hỏi -->
              <div class="table-responsive">
                <table class="data-table print-table">
                  <thead>
                    <tr>
                      <th style="width: 50px;">#</th>
                      <th>Nội dung tiêu chí đánh giá</th>
                      <th style="width: 100px; text-align: center;">Điểm chấm</th>
                      <th style="width: 160px;">Đánh giá</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="c in ALL_CRITERIA" :key="c.id">
                      <td class="text-center font-mono">{{ c.index }}</td>
                      <td>{{ c.text }}</td>
                      <td class="text-center font-mono" style="font-weight: 700;">
                        {{ currentSelectedEval.criteriaScores ? currentSelectedEval.criteriaScores[c.id] : 4 }} / 4
                      </td>
                      <td style="font-size: 12px; color: var(--text-muted);">
                        {{ SCORE_DESCRIPTIONS[currentSelectedEval.criteriaScores ? currentSelectedEval.criteriaScores[c.id] : 4] }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="printEvaluation">
            In / Tải PDF
          </button>
          <button class="btn btn-primary" @click="closeViewModal">
            Đóng
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL NHẬP / SỬA PHIẾU ĐÁNH GIÁ (UC-16) DÀNH CHO CÁN BỘ ĐI DỰ GIỜ -->
    <div v-if="showEvaluationModal" class="modal-overlay" @click.self="closeEvaluationModal">
      <div class="modal-content" style="max-width: 900px;">
        <div class="modal-header">
          <div>
            <div class="modal-title">Phiếu đánh giá giờ giảng (UC-16)</div>
            <div style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">
              GV được dự: <strong>{{ activeEvalSession?.teacherName }}</strong> ({{ activeEvalSession?.teacherAccount }}) &bull;
              Môn: <strong>{{ activeEvalSession?.subjectName }} ({{ activeEvalSession?.subjectCode }})</strong> &bull;
              Lớp: <strong>{{ activeEvalSession?.classCode }}</strong>
            </div>
          </div>
          <button class="btn-icon" @click="closeEvaluationModal">✕</button>
        </div>

        <div class="modal-body">
          <!-- Session Details strip -->
          <div class="banner-info" style="font-size: 12px; display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 8px; margin-bottom: 14px;">
            <div>Ngày: <strong>{{ formatDate(activeEvalSession?.plannedDate) }} (Ca {{ activeEvalSession?.shift }})</strong></div>
            <div>Phòng: <strong>{{ activeEvalSession?.room || 'F201' }}</strong></div>
            <div>Người chấm: <strong>{{ currentUser?.fullName || currentUser?.account }} ({{ myEvalInfo?.roleLabel }})</strong></div>
            <div>Quy chuẩn: <strong>23 tiêu chí, tối đa 92đ, đạt ≥ 69đ (75%)</strong></div>
          </div>

          <!-- Quick Actions Bar -->
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; padding: 10px 14px; background: var(--gray-bg); border-radius: var(--radius); flex-wrap: wrap; gap: 8px;">
            <span style="font-size: 12px; font-weight: 600; color: var(--text-muted);">Thao tác nhanh cho kiểm thử:</span>
            <div style="display: flex; gap: 8px;">
              <button type="button" class="btn btn-secondary" style="font-size: 12px; padding: 4px 10px;" @click="quickFill(4)">
                Mức 4 (Xuất sắc - 92đ)
              </button>
              <button type="button" class="btn btn-secondary" style="font-size: 12px; padding: 4px 10px;" @click="quickFill(3)">
                Mức 3 (Khá - 69đ)
              </button>
              <button type="button" class="btn btn-secondary" style="font-size: 12px; padding: 4px 10px;" @click="quickFill(0)">
                Xóa điểm
              </button>
            </div>
          </div>

          <!-- 23 Criteria List Grouped by 4 Domains -->
          <div style="display: flex; flex-direction: column; gap: 14px; margin-bottom: 16px;">
            <div v-for="group in CRITERIA_GROUPS" :key="group.id" class="card" style="padding: 0; overflow: hidden; margin-bottom: 0;">
              <div style="padding: 10px 16px; background: var(--gray-bg); border-bottom: 1px solid var(--border); font-size: 13px; font-weight: 700; display: flex; align-items: center; gap: 8px;">
                <span class="badge badge-blue">{{ group.num }}</span>
                <span>{{ group.title }}</span>
                <span style="font-weight: 400; color: var(--text-muted); font-size: 12px;">({{ group.items.length }} tiêu chí &bull; Thang 0 - 4)</span>
              </div>

              <div>
                <div
                  v-for="c in group.items"
                  :key="c.id"
                  style="display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; border-bottom: 1px solid var(--border-light); gap: 16px; font-size: 13px;"
                  :style="{ background: formScores[c.id] === undefined ? '#FFFDF5' : '#FFF' }"
                >
                  <div style="flex: 1;">
                    <span style="font-weight: 600; margin-right: 6px; color: var(--text-muted);">#{{ c.index }}</span>
                    <span>{{ c.text }}</span>
                  </div>
                  <div style="display: flex; gap: 6px; flex-shrink: 0;">
                    <label
                      v-for="val in [0, 1, 2, 3, 4]"
                      :key="val"
                      class="score-pill"
                      :class="{ checked: formScores[c.id] === val }"
                      :title="SCORE_DESCRIPTIONS[val]"
                    >
                      <input
                        v-model="formScores[c.id]"
                        type="radio"
                        :name="'mg_eval_' + c.id"
                        :value="val"
                        style="display: none;"
                      />
                      <span>{{ val }}</span>
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Nhận xét chi tiết -->
          <div class="form-group" style="margin-top: 14px;">
            <label class="form-label">
              Nhận xét, ưu điểm & góp ý cho giảng viên <span style="color: var(--danger);">*</span>
            </label>
            <textarea
              v-model="formNote"
              rows="3"
              class="form-control"
              placeholder="Nhập nhận xét về tác phong, phương pháp truyền đạt, mức độ tiếp thu của sinh viên và các điểm cần khắc phục..."
            ></textarea>
          </div>
        </div>

        <!-- Sticky Footer with Real-time Score calculation -->
        <div class="modal-footer" style="display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 10px;">
            <span style="font-size: 13px;">
              Tổng điểm: <strong>{{ totalFormScore }}/92</strong> ({{ formPercent }}%)
            </span>
            <span
              class="badge"
              :class="formPercent >= 75 ? 'badge-green' : 'badge-red'"
            >
              {{ formPercent >= 75 ? 'ĐẠT (≥ 75%)' : 'CHƯA ĐẠT' }}
            </span>
            <span v-if="unscoredCriteriaCount > 0" style="font-size: 12px; color: var(--danger);">
              (Còn {{ unscoredCriteriaCount }}/23 câu chưa chọn)
            </span>
          </div>

          <div style="display: flex; gap: 8px;">
            <button class="btn btn-secondary" @click="closeEvaluationModal">
              Hủy
            </button>
            <button
              class="btn btn-primary"
              :disabled="submittingEval || unscoredCriteriaCount > 0"
              @click="submitEvaluationForm"
            >
              {{ submittingEval ? 'Đang gửi...' : 'Nộp phiếu đánh giá' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'

const currentUser = ref({})

const SCORE_DESCRIPTIONS = {
  0: '0 - Chưa thực hiện',
  1: '1 - Yếu / Kém',
  2: '2 - Đạt trung bình',
  3: '3 - Khá / Tốt',
  4: '4 - Xuất sắc'
}

const CRITERIA_GROUPS = [
  {
    id: 'g1',
    num: 'I',
    title: 'Kế hoạch & Chuẩn bị bài giảng',
    items: [
      { id: 'c1', index: 1, text: 'Giáo án, bài giảng chuẩn bị đầy đủ, đúng đề cương chi tiết môn học' },
      { id: 'c2', index: 2, text: 'Chuẩn bị tài liệu học tập, học liệu số và công cụ hỗ trợ giảng dạy' },
      { id: 'c3', index: 3, text: 'Đúng giờ lên lớp, tác phong chuyên nghiệp, trang phục lịch sự' },
      { id: 'c4', index: 4, text: 'Quản lý sĩ số lớp học, điểm danh sinh viên đúng quy định' }
    ]
  },
  {
    id: 'g2',
    num: 'II',
    title: 'Nội dung & Phương pháp giảng dạy',
    items: [
      { id: 'c5', index: 5, text: 'Giới thiệu rõ ràng mục tiêu và nội dung trọng tâm của buổi học' },
      { id: 'c6', index: 6, text: 'Kiến thức truyền đạt chính xác, cập nhật và có tính thực tiễn cao' },
      { id: 'c7', index: 7, text: 'Phương pháp giảng dạy phù hợp (thuyết trình, thảo luận, làm mẫu)' },
      { id: 'c8', index: 8, text: 'Hướng dẫn sinh viên thực hành bài tập/lab cụ thể, rõ ràng' },
      { id: 'c9', index: 9, text: 'Sử dụng ví dụ thực tế phong phú, gắn liền với doanh nghiệp' },
      { id: 'c10', index: 10, text: 'Tạo không khí học tập tích cực, thu hút và lôi cuốn người học' },
      { id: 'c11', index: 11, text: 'Đặt câu hỏi kích thích tư duy và khuyến khích sinh viên phản hồi' },
      { id: 'c12', index: 12, text: 'Theo dõi, quan sát và hỗ trợ từng nhóm sinh viên kịp thời' },
      { id: 'c13', index: 13, text: 'Phân bổ thời gian giữa lý thuyết và thực hành hợp lý' }
    ]
  },
  {
    id: 'g3',
    num: 'III',
    title: 'Kỹ năng sư phạm & Quản lý lớp học',
    items: [
      { id: 'c14', index: 14, text: 'Giọng nói rõ ràng, mạch lạc, truyền cảm, tốc độ phù hợp' },
      { id: 'c15', index: 15, text: 'Sử dụng bảng, slide và công cụ trình chiếu khoa học, thẩm mỹ' },
      { id: 'c16', index: 16, text: 'Kỹ năng lắng nghe, tôn trọng và giải đáp thắc mắc của sinh viên' },
      { id: 'c17', index: 17, text: 'Xử lý linh hoạt và hiệu quả các tình huống phát sinh trong lớp' },
      { id: 'c18', index: 18, text: 'Kiểm soát trật tự và kỷ luật lớp học tốt' }
    ]
  },
  {
    id: 'g4',
    num: 'IV',
    title: 'Đánh giá & Tổng kết buổi học',
    items: [
      { id: 'c19', index: 19, text: 'Kiểm tra mức độ hiểu bài và nắm bắt kiến thức của sinh viên' },
      { id: 'c20', index: 20, text: 'Đánh giá và nhận xét công tâm kết quả thực hành của sinh viên' },
      { id: 'c21', index: 21, text: 'Tổng kết và củng cố các nội dung trọng tâm của buổi học' },
      { id: 'c22', index: 22, text: 'Giao bài tập về nhà và hướng dẫn tự học cho buổi tiếp theo' },
      { id: 'c23', index: 23, text: 'Nhắc nhở tiến độ hoàn thành Assignment / Dự án môn học' }
    ]
  }
]

const ALL_CRITERIA = CRITERIA_GROUPS.flatMap(g => g.items)

// State
const loading = ref(false)
const sessions = ref([])
const totalSessions = ref(0)
const overdueCount = ref(0)
const completedCount = ref(0)
const pendingCount = ref(0)
const allowedFaculties = ref([])

const semesters = ref([
  { id: 1, name: 'Fall 2026' },
  { id: 2, name: 'Summer 2026' },
  { id: 3, name: 'Spring 2026' }
])

const filters = reactive({
  semesterId: '',
  block: '',
  facultyId: '',
  statusFilter: 'ALL',
  search: ''
})

// Modal View state
const showViewModal = ref(false)
const loadingView = ref(false)
const activeSession = ref(null)
const sessionEvaluations = ref([])
const selectedEvalIndex = ref(0)

// Modal Nhập phiếu cá nhân cho người đi dự giờ
const showEvaluationModal = ref(false)
const activeEvalSession = ref(null)
const myEvalInfo = ref(null)
const formScores = reactive({})
const formNote = ref('')
const submittingEval = ref(false)

const currentSelectedEval = computed(() => {
  if (sessionEvaluations.value.length === 0) return null
  return sessionEvaluations.value[selectedEvalIndex.value] || sessionEvaluations.value[0]
})

const unscoredCriteriaCount = computed(() => {
  return ALL_CRITERIA.filter(c => formScores[c.id] === undefined).length
})

const totalFormScore = computed(() => {
  return Object.values(formScores).reduce((acc, val) => acc + (Number(val) || 0), 0)
})

const formPercent = computed(() => {
  return Math.round((totalFormScore.value / 92) * 100)
})

function getMyInspectorAssignment(session) {
  if (!session || !currentUser.value?.account) return null
  return (session.assignedInspectors || []).find(ins => ins.account === currentUser.value.account) || null
}

function quickFill(scoreVal) {
  ALL_CRITERIA.forEach(c => {
    formScores[c.id] = scoreVal
  })
  if (!formNote.value) {
    formNote.value = scoreVal >= 3
      ? 'Giảng viên chuẩn bị bài chu đáo, truyền đạt mạch lạc và kiểm soát lớp tốt.'
      : 'Giảng viên cần tương tác nhiều hơn với sinh viên và củng cố kiến thức cuối giờ.'
  }
}

async function openEvaluationModal(session, inspectorInfo) {
  activeEvalSession.value = session
  myEvalInfo.value = inspectorInfo
  for (const key in formScores) delete formScores[key]
  formNote.value = ''

  if (inspectorInfo && inspectorInfo.evaluation) {
    const ev = inspectorInfo.evaluation
    formNote.value = ev.note || ''
    if (ev.criteriaScores) {
      Object.assign(formScores, ev.criteriaScores)
    }
  } else {
    // Quick default
    quickFill(4)
  }
  showEvaluationModal.value = true
}

function closeEvaluationModal() {
  showEvaluationModal.value = false
  activeEvalSession.value = null
  myEvalInfo.value = null
}

async function submitEvaluationForm() {
  if (unscoredCriteriaCount.value > 0) {
    toast.warning(`Bạn còn ${unscoredCriteriaCount.value} tiêu chí chưa chấm điểm!`)
    return
  }
  if (!formNote.value.trim()) {
    toast.warning('Vui lòng nhập nhận xét chi tiết cho tiết dạy!')
    return
  }

  submittingEval.value = true
  try {
    await api.submitEvaluation({
      sessionId: activeEvalSession.value.id,
      inspectorAccount: currentUser.value.account,
      score: totalFormScore.value,
      criteriaScores: { ...formScores },
      note: formNote.value.trim()
    })
    toast.success('Đã nộp phiếu đánh giá dự giờ thành công!')
    closeEvaluationModal()
    await fetchData()
  } catch (err) {
    toast.error('Lỗi khi nộp phiếu: ' + err.message)
  } finally {
    submittingEval.value = false
  }
}

let debounceTimer = null
function debounceFetch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    fetchData()
  }, 300)
}

function clearSearch() {
  filters.search = ''
  fetchData()
}

function filterOverdue() {
  filters.statusFilter = 'OVERDUE'
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await api.getManagementInspections({
      semesterId: filters.semesterId,
      block: filters.block,
      facultyId: filters.facultyId,
      statusFilter: filters.statusFilter,
      search: filters.search
    })
    sessions.value = res.sessions || []
    totalSessions.value = res.totalSessions || 0
    overdueCount.value = res.overdueCount || 0
    completedCount.value = res.completedCount || 0
    pendingCount.value = res.pendingCount || 0
    allowedFaculties.value = res.allowedFaculties || []

    if (allowedFaculties.value.length === 1 && !filters.facultyId) {
      filters.facultyId = allowedFaculties.value[0].id
    }
  } catch (err) {
    toast.error('Lỗi tải kết quả dự giờ: ' + err.message)
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const parts = dateStr.split('-')
    if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`
    return dateStr
  } catch {
    return dateStr
  }
}

function formatDateTime(dtStr) {
  if (!dtStr) return '-'
  try {
    const d = new Date(dtStr)
    return `${d.toLocaleDateString('vi-VN')} lúc ${d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`
  } catch {
    return dtStr
  }
}

async function openViewModal(session) {
  activeSession.value = session
  selectedEvalIndex.value = 0
  showViewModal.value = true
  loadingView.value = true
  try {
    sessionEvaluations.value = await api.getSessionEvaluations(session.id)
  } catch (err) {
    toast.error('Lỗi tải phiếu đánh giá: ' + err.message)
    sessionEvaluations.value = []
  } finally {
    loadingView.value = false
  }
}

function closeViewModal() {
  showViewModal.value = false
  activeSession.value = null
  sessionEvaluations.value = []
}

function printEvaluation() {
  window.print()
}

onMounted(async () => {
  try {
    currentUser.value = await api.getMe()
  } catch (e) {
    console.error(e)
  }
  fetchData()
})
</script>

<style scoped>
.score-pill {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: #FFF;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
}

.score-pill:hover {
  background: var(--gray-bg);
}

.score-pill.checked {
  background: var(--primary);
  color: #FFF;
  border-color: var(--primary);
}

@media print {
  body * {
    visibility: hidden;
  }
  .printable-area, .printable-area * {
    visibility: visible;
  }
  .printable-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
}
</style>
