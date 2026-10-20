<template>
  <div class="my-inspections-view">
    <!-- Header -->
    <div class="view-header">
      <div>
        <h1 class="view-title">Lịch dự giờ của tôi</h1>
        <p class="view-desc">
          Theo dõi các buổi dự giờ bạn được phân công đánh giá hoặc tiết dạy của bạn được dự giờ. Hoàn thành phiếu đánh giá trong vòng 72 giờ theo quy định (GBR-04, GBR-05).
        </p>
      </div>
      <div class="header-actions">
        <button class="btn btn-outline" @click="fetchData">
          🔄 Làm mới dữ liệu
        </button>
      </div>
    </div>

    <!-- Cảnh báo quá hạn SLA 72h (GBR-05) -->
    <div v-if="overdueCount > 0" class="overdue-banner">
      <div class="overdue-icon">⚠️</div>
      <div class="overdue-content">
        <div class="overdue-title">Cảnh báo vi phạm SLA 72 giờ</div>
        <div class="overdue-text">
          Bạn có <strong>{{ overdueCount }}</strong> buổi dự giờ đã kết thúc quá hạn 72 giờ nhưng chưa gửi phiếu đánh giá! Vui lòng hoàn thành phiếu để đảm bảo tiến độ nghiệm thu giờ giảng.
        </div>
      </div>
      <button class="btn btn-danger-banner" @click="filterOverdue">
        Xem buổi quá hạn ({{ overdueCount }})
      </button>
    </div>

    <!-- Filter Card -->
    <div class="filter-card">
      <div class="filter-grid">
        <div class="filter-group">
          <label>Học kỳ</label>
          <select v-model="filters.semesterId" @change="fetchData">
            <option value="">Tất cả học kỳ</option>
            <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Block</label>
          <select v-model="filters.block" @change="fetchData">
            <option value="">Tất cả</option>
            <option value="1">Block 1</option>
            <option value="2">Block 2</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Trạng thái phiếu</label>
          <select v-model="filters.statusFilter" @change="fetchData">
            <option value="ALL">Tất cả trạng thái</option>
            <template v-if="activeTab === 'INSPECTOR'">
              <option value="UNSUBMITTED">Chưa gửi phiếu</option>
              <option value="SUBMITTED">Đã gửi phiếu</option>
              <option value="OVERDUE">⚠️ Quá hạn 72h</option>
            </template>
            <template v-else>
              <option value="HAS_EVAL">Đã có phiếu đánh giá</option>
              <option value="NO_EVAL">Đang chờ phiếu</option>
            </template>
          </select>
        </div>

        <div class="filter-group filter-search">
          <label>Tìm kiếm nhanh</label>
          <div class="search-input-wrapper">
            <input
              v-model="filters.search"
              type="text"
              placeholder="Tìm theo lớp, mã môn, giảng viên..."
              @input="debounceFetch"
            />
            <span v-if="filters.search" class="clear-search" @click="clearSearch">✕</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs Navigation -->
    <div class="tabs-container">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'INSPECTOR' }"
        @click="switchTab('INSPECTOR')"
      >
        <span>👨‍🏫 Tôi đi dự giờ</span>
        <span class="tab-badge">{{ totalInspector }}</span>
        <span v-if="overdueCount > 0" class="badge-overdue-dot" title="Có buổi quá hạn 72h">⚠️</span>
      </button>

      <button
        class="tab-btn"
        :class="{ active: activeTab === 'TEACHER' }"
        @click="switchTab('TEACHER')"
      >
        <span>🎯 Tôi được dự giờ</span>
        <span class="tab-badge">{{ totalTeacher }}</span>
      </button>
    </div>

    <!-- TAB 1: TÔI ĐI DỰ GIỜ -->
    <div v-if="activeTab === 'INSPECTOR'" class="tab-content card">
      <div v-if="loading" class="empty-state">Đang tải lịch dự giờ...</div>
      <div v-else-if="asInspectorList.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <p>Không có buổi dự giờ nào phù hợp với bộ lọc.</p>
      </div>
      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th style="width: 170px;">Mã kế hoạch</th>
              <th>Giảng viên dạy</th>
              <th>Môn học & Lớp</th>
              <th style="width: 170px;">Thời gian & Phòng</th>
              <th style="width: 90px; text-align: center;">Vai trò</th>
              <th style="width: 160px; text-align: center;">Trạng thái phiếu</th>
              <th style="width: 140px; text-align: center;">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(item, idx) in asInspectorList"
              :key="item.id"
              :class="{ 'row-overdue': item.isOverdue }"
            >
              <td class="text-center font-mono">{{ idx + 1 }}</td>
              <td>
                <!-- GBR-02: Không tạo link để tránh lộ toàn bộ kế hoạch khoa -->
                <span class="plan-code-badge" :title="`Kế hoạch ${item.planCode} (${item.facultyName})`">
                  {{ item.planCode }}
                </span>
              </td>
              <td>
                <div class="teacher-info">
                  <div class="teacher-name">{{ item.teacherName }}</div>
                  <div class="teacher-sub font-mono">({{ item.teacherAccount }})</div>
                </div>
              </td>
              <td>
                <div class="subject-title">{{ item.subjectName }}</div>
                <div class="subject-meta font-mono">
                  <span class="badge-code">{{ item.subjectCode }}</span>
                  <span class="badge-class">{{ item.classCode }}</span>
                </div>
              </td>
              <td>
                <div class="time-primary">📅 {{ formatDate(item.plannedDate) }}</div>
                <div class="time-sub">Ca {{ item.shift }} &bull; P.{{ item.room || 'F201' }}</div>
              </td>
              <td class="text-center">
                <span class="badge-role" :class="getRoleClass(item.myRole)">
                  {{ item.myRole }}
                </span>
              </td>
              <td class="text-center">
                <span v-if="item.isOverdue" class="badge-status badge-danger">
                  ⚠️ Quá hạn 72h
                </span>
                <span v-else-if="item.isSubmitted" class="badge-status badge-success">
                  ✓ Đã gửi ({{ item.evaluation?.score }}/92đ)
                </span>
                <span v-else class="badge-status badge-warning">
                  ⏳ Chưa gửi phiếu
                </span>
              </td>
              <td class="text-center">
                <button
                  v-if="!item.isSubmitted"
                  class="btn-action btn-evaluate"
                  @click="openEvaluationModal(item)"
                >
                  ✍️ Nhập phiếu
                </button>
                <button
                  v-else
                  class="btn-action btn-view"
                  @click="openViewModal(item)"
                >
                  👁️ Xem phiếu
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- TAB 2: TÔI ĐƯỢC DỰ GIỜ -->
    <div v-if="activeTab === 'TEACHER'" class="tab-content card">
      <div v-if="loading" class="empty-state">Đang tải lịch dự giờ...</div>
      <div v-else-if="asTeacherList.length === 0" class="empty-state">
        <div class="empty-icon">🎯</div>
        <p>Bạn chưa có buổi dạy nào được phân công dự giờ trong kỳ này.</p>
      </div>
      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px;">STT</th>
              <th style="width: 170px;">Mã kế hoạch</th>
              <th>Môn học & Lớp dạy</th>
              <th style="width: 170px;">Thời gian & Phòng</th>
              <th>Người dự giờ phân công</th>
              <th style="width: 180px; text-align: center;">Kết quả đánh giá</th>
              <th style="width: 140px; text-align: center;">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in asTeacherList" :key="item.id">
              <td class="text-center font-mono">{{ idx + 1 }}</td>
              <td>
                <span class="plan-code-badge" :title="`Kế hoạch ${item.planCode} (${item.facultyName})`">
                  {{ item.planCode }}
                </span>
              </td>
              <td>
                <div class="subject-title">{{ item.subjectName }}</div>
                <div class="subject-meta font-mono">
                  <span class="badge-code">{{ item.subjectCode }}</span>
                  <span class="badge-class">{{ item.classCode }}</span>
                </div>
              </td>
              <td>
                <div class="time-primary">📅 {{ formatDate(item.plannedDate) }}</div>
                <div class="time-sub">Ca {{ item.shift }} &bull; P.{{ item.room || 'F201' }}</div>
              </td>
              <td>
                <div class="inspectors-list">
                  <div v-if="item.inspector1" class="inspector-chip" :class="{ submitted: item.inspector1Submitted }">
                    <span class="chip-role">GV1:</span>
                    <span>{{ item.inspector1.fullName }}</span>
                    <span class="chip-status">{{ item.inspector1Submitted ? '✓ Đã nộp' : '⏳ Chưa nộp' }}</span>
                  </div>
                  <div v-if="item.inspector2" class="inspector-chip" :class="{ submitted: item.inspector2Submitted }">
                    <span class="chip-role">GV2:</span>
                    <span>{{ item.inspector2.fullName }}</span>
                    <span class="chip-status">{{ item.inspector2Submitted ? '✓ Đã nộp' : '⏳ Chưa nộp' }}</span>
                  </div>
                  <div v-if="item.inspector3" class="inspector-chip" :class="{ submitted: item.inspector3Submitted }">
                    <span class="chip-role">GV3:</span>
                    <span>{{ item.inspector3.fullName }}</span>
                    <span class="chip-status">{{ item.inspector3Submitted ? '✓ Đã nộp' : '⏳ Chưa nộp' }}</span>
                  </div>
                </div>
              </td>
              <td class="text-center">
                <template v-if="item.hasEvaluations">
                  <div class="score-summary">
                    <span class="score-value font-mono">{{ getAverageScore(item.evaluations) }}/92đ</span>
                    <span class="score-percent">({{ getAveragePercent(item.evaluations) }}%)</span>
                  </div>
                  <span
                    class="badge-result"
                    :class="getAveragePercent(item.evaluations) >= 75 ? 'badge-success' : 'badge-danger'"
                  >
                    {{ getAveragePercent(item.evaluations) >= 75 ? 'ĐẠT (≥ 75%)' : 'CHƯA ĐẠT' }}
                  </span>
                </template>
                <template v-else>
                  <span class="badge-status badge-neutral">
                    ⏳ Đang chờ đánh giá
                  </span>
                </template>
              </td>
              <td class="text-center">
                <button
                  class="btn-action btn-view"
                  :disabled="!item.hasEvaluations"
                  :title="item.hasEvaluations ? 'Xem chi tiết phiếu đánh giá' : 'Chưa có phiếu đánh giá nào được nộp'"
                  @click="openViewModal(item)"
                >
                  👁️ Xem phiếu ({{ item.evaluations?.length || 0 }})
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL 1: NHẬP PHIẾU ĐÁNH GIÁ (UC-16, 23 TIÊU CHÍ, GBR-04) -->
    <div v-if="showEvaluationModal" class="modal-backdrop" @click.self="closeEvaluationModal">
      <div class="modal-card modal-lg">
        <div class="modal-header">
          <div>
            <h2 class="modal-title">Phiếu Đánh Giá Giờ Giảng (UC-16)</h2>
            <div class="modal-sub">
              Giảng viên: <strong>{{ activeSession?.teacherName }}</strong> ({{ activeSession?.teacherAccount }}) &bull;
              Môn: <strong>{{ activeSession?.subjectName }} ({{ activeSession?.subjectCode }})</strong> &bull;
              Lớp: <strong>{{ activeSession?.classCode }}</strong>
            </div>
          </div>
          <button class="modal-close" @click="closeEvaluationModal">&times;</button>
        </div>

        <div class="modal-body evaluation-body">
          <!-- Session Details strip -->
          <div class="session-strip">
            <div class="strip-item">
              <span class="strip-label">Ngày dự:</span>
              <span class="strip-val">{{ formatDate(activeSession?.plannedDate) }} (Ca {{ activeSession?.shift }})</span>
            </div>
            <div class="strip-item">
              <span class="strip-label">Phòng:</span>
              <span class="strip-val">{{ activeSession?.room || 'F201' }}</span>
            </div>
            <div class="strip-item">
              <span class="strip-label">Người chấm:</span>
              <span class="strip-val">{{ currentUserName }} ({{ activeSession?.myRole }})</span>
            </div>
            <div class="strip-item">
              <span class="strip-label">Quy chuẩn:</span>
              <span class="strip-val">GBR-04: 23 tiêu chí, tối đa 92 điểm, đạt khi ≥ 69 điểm (75%)</span>
            </div>
          </div>

          <!-- Quick Actions Bar -->
          <div class="quick-fill-bar">
            <span class="quick-label">⚡ Thao tác nhanh cho kiểm thử:</span>
            <button type="button" class="btn-quick" @click="quickFill(4)">
              Điền mẫu Mức 4 (Xuất sắc - 92đ)
            </button>
            <button type="button" class="btn-quick" @click="quickFill(3)">
              Điền mẫu Mức 3 (Khá - 69đ)
            </button>
            <button type="button" class="btn-quick btn-quick-reset" @click="quickFill(0)">
              Xóa điểm
            </button>
          </div>

          <!-- 23 Criteria List Grouped by 4 Domains -->
          <div class="criteria-list">
            <div v-for="group in CRITERIA_GROUPS" :key="group.id" class="criteria-group">
              <div class="group-header">
                <span class="group-num">{{ group.num }}</span>
                <span class="group-title">{{ group.title }}</span>
                <span class="group-weight">({{ group.items.length }} tiêu chí &bull; Thang 0 - 4)</span>
              </div>

              <div class="group-items">
                <div
                  v-for="c in group.items"
                  :key="c.id"
                  class="criteria-row"
                  :class="{ 'criteria-unscored': formScores[c.id] === undefined }"
                >
                  <div class="criteria-left">
                    <span class="criteria-id">#{{ c.index }}</span>
                    <span class="criteria-text">{{ c.text }}</span>
                  </div>
                  <div class="criteria-right">
                    <div class="score-options">
                      <label
                        v-for="val in [0, 1, 2, 3, 4]"
                        :key="val"
                        class="score-radio-label"
                        :class="{ checked: formScores[c.id] === val }"
                      >
                        <input
                          v-model="formScores[c.id]"
                          type="radio"
                          :name="c.id"
                          :value="val"
                        />
                        <span class="score-val">{{ val }}</span>
                        <span class="score-desc">{{ SCORE_DESCRIPTIONS[val] }}</span>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Nhận xét chi tiết -->
          <div class="notes-section">
            <label class="notes-label">
              Nhận xét, ưu điểm & góp ý cho giảng viên <span class="required">*</span>
            </label>
            <textarea
              v-model="formNote"
              rows="4"
              class="notes-textarea"
              placeholder="Nhập nhận xét về tác phong, phương pháp truyền đạt, mức độ tiếp thu của sinh viên và các điểm cần khắc phục..."
            ></textarea>
          </div>
        </div>

        <!-- Sticky Footer with Real-time Score calculation -->
        <div class="modal-footer evaluation-footer">
          <div class="live-score-box">
            <div class="live-score-main">
              Tổng điểm: <strong class="font-mono">{{ totalFormScore }}/92</strong>
              <span class="live-percent">({{ formPercent }}%)</span>
            </div>
            <div
              class="live-status-badge"
              :class="formPercent >= 75 ? 'badge-success' : 'badge-danger'"
            >
              {{ formPercent >= 75 ? '✓ ĐẠT YÊU CẦU (≥ 75%)' : '✕ CHƯA ĐẠT YÊU CẦU' }}
            </div>
            <div class="unscored-count" v-if="unscoredCriteriaCount > 0">
              (Còn {{ unscoredCriteriaCount }}/23 câu chưa chọn)
            </div>
          </div>

          <div class="footer-buttons">
            <button class="btn btn-outline" @click="closeEvaluationModal">
              Hủy
            </button>
            <button
              class="btn btn-primary"
              :disabled="submitting || unscoredCriteriaCount > 0"
              @click="submitEvaluationForm"
            >
              {{ submitting ? 'Đang gửi...' : 'Nộp phiếu đánh giá' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL 2: XEM PHIẾU ĐÁNH GIÁ (UC-17) -->
    <div v-if="showViewModal" class="modal-backdrop" @click.self="closeViewModal">
      <div class="modal-card modal-lg">
        <div class="modal-header">
          <div>
            <h2 class="modal-title">Chi Tiết Phiếu Đánh Giá Dự Giờ (UC-17)</h2>
            <div class="modal-sub">
              Buổi dự: <strong>{{ activeSession?.classCode }} - {{ activeSession?.subjectName }}</strong> &bull;
              GV dạy: <strong>{{ activeSession?.teacherName }}</strong>
            </div>
          </div>
          <button class="modal-close" @click="closeViewModal">&times;</button>
        </div>

        <div class="modal-body printable-area">
          <div v-if="loadingView" class="empty-state">Đang tải phiếu đánh giá...</div>
          <div v-else-if="sessionEvaluations.length === 0" class="empty-state">
            <p>Chưa có phiếu đánh giá nào được nộp cho buổi dự giờ này.</p>
          </div>
          <div v-else>
            <!-- Inspector selector tabs if multiple evaluations -->
            <div class="eval-tabs">
              <button
                v-for="(ev, index) in sessionEvaluations"
                :key="ev.id"
                class="eval-tab-btn"
                :class="{ active: selectedEvalIndex === index }"
                @click="selectedEvalIndex = index"
              >
                <span>Phiếu {{ index + 1 }}: {{ ev.inspectorName }} ({{ ev.inspectorAccount }})</span>
                <span class="eval-score-pill font-mono">{{ ev.score }}/92đ ({{ ev.percentage }}%)</span>
              </button>
            </div>

            <!-- Current Evaluation Summary -->
            <div v-if="currentSelectedEval" class="eval-detail-sheet">
              <div class="eval-meta-grid">
                <div><strong>Người đánh giá:</strong> {{ currentSelectedEval.inspectorName }} ({{ currentSelectedEval.inspectorAccount }})</div>
                <div><strong>Thời gian gửi phiếu:</strong> {{ formatDateTime(currentSelectedEval.submittedAt) }}</div>
                <div><strong>Tổng điểm:</strong> <span class="font-mono text-bold">{{ currentSelectedEval.score }} / 92 điểm</span></div>
                <div>
                  <strong>Kết quả:</strong>
                  <span
                    class="badge-result"
                    :class="currentSelectedEval.percentage >= 75 ? 'badge-success' : 'badge-danger'"
                  >
                    {{ currentSelectedEval.percentage }}% &bull; {{ currentSelectedEval.percentage >= 75 ? 'ĐẠT YÊU CẦU' : 'KHÔNG ĐẠT' }}
                  </span>
                </div>
              </div>

              <!-- Nhận xét của người dự -->
              <div class="eval-comment-box">
                <div class="comment-heading">📝 Nhận xét & Đánh giá chung:</div>
                <div class="comment-body">{{ currentSelectedEval.note || 'Không có nhận xét thêm.' }}</div>
              </div>

              <!-- Chi tiết điểm 23 câu hỏi -->
              <div class="eval-criteria-table-wrap">
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
                      <td class="text-center font-mono text-bold">
                        {{ currentSelectedEval.criteriaScores ? currentSelectedEval.criteriaScores[c.id] : 4 }} / 4
                      </td>
                      <td>
                        <span class="score-desc-sm">
                          {{ SCORE_DESCRIPTIONS[currentSelectedEval.criteriaScores ? currentSelectedEval.criteriaScores[c.id] : 4] }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-outline" @click="printEvaluation">
            🖨️ In / Tải PDF
          </button>
          <button class="btn btn-primary" @click="closeViewModal">
            Đóng
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../services/api.js'
import { toast } from '../services/toast.js'

// ==========================================
// 23 TIÊU CHÍ ĐÁNH GIÁ (GBR-04: N = 23, MAX 92 ĐIỂM, 75% = 69 ĐIỂM)
// ==========================================
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
const activeTab = ref('INSPECTOR') // 'INSPECTOR' or 'TEACHER'
const loading = ref(false)
const asInspectorList = ref([])
const asTeacherList = ref([])
const overdueCount = ref(0)
const totalInspector = ref(0)
const totalTeacher = ref(0)

const semesters = ref([
  { id: 1, name: 'Fall 2026' },
  { id: 2, name: 'Summer 2026' },
  { id: 3, name: 'Spring 2026' }
])

const filters = reactive({
  semesterId: '',
  block: '',
  statusFilter: 'ALL',
  search: ''
})

// Current logged in user name
const currentUserName = computed(() => {
  const token = localStorage.getItem('qldg_token') || '1:1:TBDT'
  const id = parseInt(token.split(':')[0], 10)
  return id === 6 ? 'Nguyễn Văn Nam (namnv)' : 'Giảng viên'
})

// Modal Evaluation state
const showEvaluationModal = ref(false)
const activeSession = ref(null)
const formScores = reactive({})
const formNote = ref('')
const submitting = ref(false)

// Modal View state
const showViewModal = ref(false)
const loadingView = ref(false)
const sessionEvaluations = ref([])
const selectedEvalIndex = ref(0)

const currentSelectedEval = computed(() => {
  if (sessionEvaluations.value.length === 0) return null
  return sessionEvaluations.value[selectedEvalIndex.value] || sessionEvaluations.value[0]
})

// Calculations for active form
const totalFormScore = computed(() => {
  let sum = 0
  for (const c of ALL_CRITERIA) {
    const val = formScores[c.id]
    if (typeof val === 'number') sum += val
  }
  return sum
})

const formPercent = computed(() => {
  return Math.round((totalFormScore.value / 92) * 100)
})

const unscoredCriteriaCount = computed(() => {
  return ALL_CRITERIA.filter(c => formScores[c.id] === undefined).length
})

// Methods
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

function switchTab(tab) {
  activeTab.value = tab
  filters.statusFilter = 'ALL'
  fetchData()
}

function filterOverdue() {
  activeTab.value = 'INSPECTOR'
  filters.statusFilter = 'OVERDUE'
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await api.getMyInspections({
      semesterId: filters.semesterId,
      block: filters.block,
      statusFilter: filters.statusFilter,
      search: filters.search
    })
    asInspectorList.value = res.asInspector || []
    asTeacherList.value = res.asTeacher || []
    overdueCount.value = res.overdueCount || 0
    totalInspector.value = res.totalInspector || 0
    totalTeacher.value = res.totalTeacher || 0
  } catch (err) {
    toast.error('Lỗi tải lịch dự giờ: ' + err.message)
  } finally {
    loading.value = false
  }
}

function getRoleClass(role) {
  if (role === 'GV 1') return 'role-primary'
  if (role === 'GV 2') return 'role-secondary'
  return 'role-tertiary'
}

function getAverageScore(evals) {
  if (!evals || evals.length === 0) return 0
  const sum = evals.reduce((acc, cur) => acc + (cur.score || 0), 0)
  return Math.round(sum / evals.length)
}

function getAveragePercent(evals) {
  if (!evals || evals.length === 0) return 0
  const avg = getAverageScore(evals)
  return Math.round((avg / 92) * 100)
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

// Evaluation Modal Handlers
function openEvaluationModal(item) {
  activeSession.value = item
  // Reset form
  for (const c of ALL_CRITERIA) {
    delete formScores[c.id]
  }
  formNote.value = ''
  showEvaluationModal.value = true
}

function closeEvaluationModal() {
  showEvaluationModal.value = false
  activeSession.value = null
}

function quickFill(score) {
  for (const c of ALL_CRITERIA) {
    formScores[c.id] = score
  }
  if (score === 4) {
    formNote.value = 'Giảng viên chuẩn bị bài giảng chu đáo, kiến thức chuyên môn vững vàng, sinh viên tích cực tương tác và hoàn thành tốt bài lab.'
  } else if (score === 3) {
    formNote.value = 'Nội dung bám sát đề cương chi tiết, phương pháp giảng giải dễ hiểu. Cần khuyến khích thêm các sinh viên ngồi bàn cuối phát biểu.'
  } else {
    formNote.value = ''
  }
}

async function submitEvaluationForm() {
  if (unscoredCriteriaCount.value > 0) {
    toast.warning(`Vui lòng chấm điểm đầy đủ cả 23 tiêu chí! (Còn thiếu ${unscoredCriteriaCount.value} câu)`)
    return
  }
  if (!formNote.value || !formNote.value.trim()) {
    toast.warning('Vui lòng nhập nhận xét chi tiết cho giảng viên!')
    return
  }

  submitting.value = true
  try {
    const token = localStorage.getItem('qldg_token') || '6:1:GV'
    const account = token.split(':')[0] === '6' ? 'namnv' : 'namnv'
    await api.submitEvaluation({
      sessionId: activeSession.value.id,
      inspectorAccount: account,
      score: totalFormScore.value,
      criteriaScores: { ...formScores },
      note: formNote.value
    })
    toast.success(`Đã gửi phiếu đánh giá thành công! Tổng điểm: ${totalFormScore.value}/92đ (${formPercent.value}%)`)
    closeEvaluationModal()
    fetchData()
  } catch (err) {
    toast.error('Lỗi gửi phiếu đánh giá: ' + err.message)
  } finally {
    submitting.value = false
  }
}

// View Modal Handlers
async function openViewModal(item) {
  activeSession.value = item
  selectedEvalIndex.value = 0
  showViewModal.value = true
  loadingView.value = true
  try {
    sessionEvaluations.value = await api.getSessionEvaluations(item.id)
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

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.my-inspections-view {
  max-width: 1200px;
  margin: 0 auto;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.view-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 6px;
}

.view-desc {
  font-size: 13px;
  color: var(--text-muted);
  max-width: 800px;
  line-height: 1.5;
}

/* Overdue SLA Banner */
.overdue-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-left: 5px solid #F59E0B;
  border-radius: var(--radius);
  padding: 14px 18px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(245, 158, 11, 0.1);
}

.overdue-icon {
  font-size: 24px;
}

.overdue-content {
  flex: 1;
}

.overdue-title {
  font-weight: 700;
  font-size: 14px;
  color: #B45309;
  margin-bottom: 2px;
}

.overdue-text {
  font-size: 13px;
  color: #92400E;
}

.btn-danger-banner {
  background: #DC2626;
  color: #FFF;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.btn-danger-banner:hover {
  background: #B91C1C;
}

/* Filter Card */
.filter-card {
  background: #FFF;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow);
}

.filter-grid {
  display: grid;
  grid-template-columns: 180px 140px 200px 1fr;
  gap: 16px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-group label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.filter-group select,
.filter-group input {
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  background: #FFF;
  transition: border-color 0.15s;
}

.filter-group select:focus,
.filter-group input:focus {
  border-color: var(--primary);
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input-wrapper input {
  width: 100%;
  padding-right: 30px;
}

.clear-search {
  position: absolute;
  right: 10px;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 12px;
}

/* Tabs Navigation */
.tabs-container {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid var(--border);
  margin-bottom: 16px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn:hover {
  color: var(--primary);
}

.tab-btn.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.tab-badge {
  background: #F3F4F6;
  color: #374151;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 12px;
  font-weight: 700;
}

.tab-btn.active .tab-badge {
  background: var(--primary-bg);
  color: var(--primary);
}

.badge-overdue-dot {
  font-size: 12px;
}

/* Tables */
.card {
  background: #FFF;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  background: #F9FAFB;
  padding: 12px 14px;
  text-align: left;
  font-weight: 600;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border);
}

.data-table td {
  padding: 14px;
  border-bottom: 1px solid var(--border-light);
  vertical-align: middle;
}

.data-table tr:hover {
  background: #F9FAFB;
}

.row-overdue {
  background: #FEF2F2 !important;
}

.plan-code-badge {
  font-family: monospace;
  font-weight: 600;
  font-size: 12px;
  background: #F3F4F6;
  color: #374151;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #E5E7EB;
  display: inline-block;
  user-select: all;
}

.teacher-name {
  font-weight: 600;
  color: var(--text-main);
}

.teacher-sub {
  font-size: 12px;
  color: var(--text-muted);
}

.subject-title {
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}

.subject-meta {
  display: flex;
  gap: 6px;
  font-size: 11px;
}

.badge-code {
  background: #EFF6FF;
  color: #1D4ED8;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.badge-class {
  background: #F3F4F6;
  color: #374151;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.time-primary {
  font-weight: 600;
  color: var(--text-main);
}

.time-sub {
  font-size: 12px;
  color: var(--text-muted);
}

.badge-role {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
}

.role-primary {
  background: #EFF6FF;
  color: #1D4ED8;
  border: 1px solid #BFDBFE;
}

.role-secondary {
  background: #F5F3FF;
  color: #6D28D9;
  border: 1px solid #DDD6FE;
}

.role-tertiary {
  background: #F0FDF4;
  color: #15803D;
  border: 1px solid #BBF7D0;
}

.badge-status {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.badge-danger {
  background: #FEF2F2;
  color: #DC2626;
  border: 1px solid #FECACA;
}

.badge-success {
  background: #F0FDF4;
  color: #16A34A;
  border: 1px solid #BBF7D0;
}

.badge-warning {
  background: #FFFBEB;
  color: #D97706;
  border: 1px solid #FDE68A;
}

.badge-neutral {
  background: #F3F4F6;
  color: #6B7280;
}

.btn-action {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  border: none;
}

.btn-evaluate {
  background: var(--primary);
  color: #FFF;
}

.btn-evaluate:hover {
  background: var(--primary-hover);
}

.btn-view {
  background: #F3F4F6;
  color: #374151;
  border: 1px solid var(--border);
}

.btn-view:hover:not(:disabled) {
  background: #E5E7EB;
}

.btn-view:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Tab 2: Chip list for inspectors */
.inspectors-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.inspector-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 3px 8px;
  background: #F9FAFB;
  border-radius: 4px;
  border: 1px solid var(--border-light);
}

.inspector-chip.submitted {
  background: #F0FDF4;
  border-color: #DCFCE7;
}

.chip-role {
  font-weight: 700;
  color: var(--text-muted);
}

.chip-status {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted);
}

.inspector-chip.submitted .chip-status {
  color: #16A34A;
  font-weight: 600;
}

.score-summary {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 2px;
}

.score-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
}

.score-percent {
  font-size: 11px;
  color: var(--text-muted);
}

.badge-result {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 700;
}

/* Modals */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-card {
  background: #FFF;
  border-radius: var(--radius);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  width: 100%;
}

.modal-lg {
  max-width: 950px;
}

.modal-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #FAFAFA;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.modal-sub {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  line-height: 1;
  color: var(--text-muted);
  cursor: pointer;
}

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 14px 24px;
  border-top: 1px solid var(--border);
  background: #FAFAFA;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Evaluation Form specifics */
.session-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  background: var(--primary-bg);
  border: 1px solid #BFDBFE;
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 12px;
  margin-bottom: 16px;
}

.strip-item {
  display: flex;
  gap: 6px;
}

.strip-label {
  color: var(--text-muted);
  font-weight: 500;
}

.strip-val {
  color: #1E3A8A;
  font-weight: 600;
}

.quick-fill-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #F3F4F6;
  border-radius: 6px;
  font-size: 12px;
  margin-bottom: 18px;
}

.quick-label {
  font-weight: 600;
  color: #4B5563;
}

.btn-quick {
  background: #FFF;
  border: 1px solid var(--border);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.btn-quick:hover {
  background: var(--primary-bg);
  color: var(--primary);
  border-color: var(--primary);
}

.btn-quick-reset {
  color: #DC2626;
}

/* Criteria List */
.criteria-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.criteria-group {
  border: 1px solid var(--border);
  border-radius: 6px;
  overflow: hidden;
}

.group-header {
  background: #F9FAFB;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-num {
  background: var(--primary);
  color: #FFF;
  font-weight: 700;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
}

.group-title {
  font-weight: 700;
  font-size: 13px;
  color: var(--text-main);
}

.group-weight {
  font-size: 11px;
  color: var(--text-muted);
}

.criteria-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-light);
  gap: 20px;
  transition: background 0.15s;
}

.criteria-row:last-child {
  border-bottom: none;
}

.criteria-row:hover {
  background: #FAFAFA;
}

.criteria-unscored {
  background: #FFFBEB;
}

.criteria-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
}

.criteria-id {
  font-family: monospace;
  font-weight: 700;
  font-size: 12px;
  color: var(--text-muted);
  width: 28px;
}

.criteria-text {
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.4;
}

.criteria-right {
  flex-shrink: 0;
}

.score-options {
  display: flex;
  gap: 6px;
}

.score-radio-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 48px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #FFF;
  cursor: pointer;
  transition: all 0.15s;
  padding: 2px;
}

.score-radio-label input {
  display: none;
}

.score-radio-label .score-val {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
}

.score-radio-label .score-desc {
  font-size: 9px;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.1;
}

.score-radio-label.checked {
  background: var(--primary-bg);
  border-color: var(--primary);
}

.score-radio-label.checked .score-val {
  color: var(--primary);
}

.notes-section {
  margin-top: 20px;
}

.notes-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-main);
}

.required {
  color: var(--danger);
}

.notes-textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  resize: vertical;
}

.notes-textarea:focus {
  border-color: var(--primary);
}

/* Evaluation Footer with Realtime score */
.evaluation-footer {
  justify-content: space-between;
  align-items: center;
}

.live-score-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.live-score-main {
  font-size: 15px;
  color: var(--text-main);
}

.live-percent {
  font-size: 13px;
  color: var(--text-muted);
}

.live-status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
}

.unscored-count {
  font-size: 11px;
  color: #DC2626;
  font-weight: 600;
}

.footer-buttons {
  display: flex;
  gap: 10px;
}

/* Modal View details */
.eval-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 8px;
}

.eval-tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #F3F4F6;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.eval-tab-btn.active {
  background: var(--primary);
  color: #FFF;
  border-color: var(--primary);
}

.eval-score-pill {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
}

.eval-tab-btn.active .eval-score-pill {
  background: rgba(255, 255, 255, 0.25);
}

.eval-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 20px;
  background: #F9FAFB;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 14px;
  font-size: 13px;
  margin-bottom: 16px;
}

.eval-comment-box {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 20px;
}

.comment-heading {
  font-weight: 700;
  font-size: 13px;
  color: #92400E;
  margin-bottom: 4px;
}

.comment-body {
  font-size: 13px;
  color: #78350F;
  line-height: 1.5;
}

.eval-criteria-table-wrap {
  border: 1px solid var(--border);
  border-radius: 6px;
  overflow: hidden;
}

.score-desc-sm {
  font-size: 12px;
  color: var(--text-muted);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

/* Print styles for UC-17 */
@media print {
  body * {
    visibility: hidden;
  }
  .printable-area,
  .printable-area * {
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
