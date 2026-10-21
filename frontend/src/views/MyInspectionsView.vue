<template>
  <div>
    <!-- Header -->
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <h1 class="page-title">Lịch dự giờ của tôi</h1>
      </div>
      <div>
        <button class="btn btn-secondary" @click="fetchData">
          Làm mới
        </button>
      </div>
    </div>

    <!-- Cảnh báo quá hạn SLA 72h -->
    <div v-if="overdueCount > 0" class="banner-warning" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <div style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 18px;">⚠️</span>
        <div>
          <strong>Cảnh báo vi phạm SLA 72 giờ:</strong> Bạn có <strong>{{ overdueCount }}</strong> buổi dự giờ đã quá hạn 72h nhưng chưa gửi phiếu đánh giá.
        </div>
      </div>
      <button class="btn btn-danger" style="padding: 4px 12px; font-size: 12px;" @click="filterOverdue">
        Lọc buổi quá hạn ({{ overdueCount }})
      </button>
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
          <label class="form-label">Trạng thái phiếu</label>
          <select v-model="filters.statusFilter" class="form-control" @change="fetchData">
            <option value="ALL">-- Tất cả trạng thái --</option>
            <template v-if="activeTab === 'INSPECTOR'">
              <option value="UNSUBMITTED">Chưa gửi phiếu</option>
              <option value="SUBMITTED">Đã gửi phiếu</option>
              <option value="OVERDUE">Quá hạn 72h</option>
            </template>
            <template v-else>
              <option value="HAS_EVAL">Đã có phiếu đánh giá</option>
              <option value="NO_EVAL">Đang chờ phiếu</option>
            </template>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Tìm kiếm nhanh</label>
          <input
            v-model="filters.search"
            class="form-control"
            placeholder="Lớp, mã môn, giảng viên..."
            @input="debounceFetch"
          />
        </div>
      </div>
    </div>

    <!-- Tabs Navigation -->
    <div class="tabs-nav">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'INSPECTOR' }"
        @click="switchTab('INSPECTOR')"
      >
        Tôi đi dự giờ ({{ totalInspector }})
      </button>

      <button
        class="tab-btn"
        :class="{ active: activeTab === 'TEACHER' }"
        @click="switchTab('TEACHER')"
      >
        Tôi được dự giờ ({{ totalTeacher }})
      </button>
    </div>

    <!-- TAB 1: TÔI ĐI DỰ GIỜ -->
    <div v-if="activeTab === 'INSPECTOR'">
      <div v-if="loading" class="card" style="text-align: center; padding: 40px; color: var(--text-muted);">
        Đang tải lịch dự giờ...
      </div>
      <div v-else-if="asInspectorList.length === 0" class="empty-state card">
        <div class="empty-icon">📋</div>
        <div class="empty-title">Không tìm thấy buổi dự giờ</div>
        <div style="font-size: 13px;">Không có buổi dự giờ nào phù hợp với bộ lọc.</div>
      </div>
      <div v-else class="card" style="padding: 0; overflow: hidden;">
        <div style="padding: 14px 20px; background: #FFF; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between;">
          <h2 class="card-title" style="margin-bottom: 0;">
            Danh sách buổi đi dự giờ
          </h2>
          <span class="badge badge-gray">{{ asInspectorList.length }} buổi</span>
        </div>

        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 50px;">STT</th>
                <th>Mã kế hoạch</th>
                <th>Giảng viên dạy</th>
                <th>Môn học & Lớp</th>
                <th>Thời gian & Phòng</th>
                <th style="text-align: center;">Vai trò</th>
                <th style="text-align: center;">Trạng thái phiếu</th>
                <th style="text-align: center;">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(item, idx) in asInspectorList"
                :key="item.id"
                :style="{ background: item.isOverdue ? 'var(--danger-bg)' : undefined }"
              >
                <td class="text-center font-mono">{{ idx + 1 }}</td>
                <td style="font-weight: 600;">
                  <span class="badge badge-gray">{{ item.planCode }}</span>
                </td>
                <td>
                  <div style="font-weight: 600; color: var(--text-main);">{{ item.teacherName }}</div>
                  <div style="font-size: 12px; color: var(--text-muted);">({{ item.teacherAccount }})</div>
                </td>
                <td>
                  <div style="font-weight: 600; color: var(--text-main); margin-bottom: 4px;">{{ item.subjectName }}</div>
                  <div style="display: flex; gap: 6px; font-size: 11px;">
                    <span class="badge badge-blue">{{ item.subjectCode }}</span>
                    <span class="badge badge-gray">{{ item.classCode }}</span>
                  </div>
                </td>
                <td>
                  <div style="font-weight: 600;">{{ formatDate(item.plannedDate) }}</div>
                  <div style="font-size: 12px; color: var(--text-muted);">Ca {{ item.shift }} &bull; P.{{ item.room || 'F201' }}</div>
                </td>
                <td style="text-align: center;">
                  <span class="badge" :class="item.myRole === 'GV 1' ? 'badge-blue' : 'badge-gray'">
                    {{ item.myRole }}
                  </span>
                </td>
                <td style="text-align: center;">
                  <span v-if="item.isOverdue" class="badge badge-red">
                    Quá hạn 72h
                  </span>
                  <span v-else-if="item.isSubmitted" class="badge badge-green">
                    Đã gửi ({{ item.evaluation?.score }}/92đ)
                  </span>
                  <span v-else class="badge badge-gray">
                    Chưa gửi phiếu
                  </span>
                </td>
                <td style="text-align: center;">
                  <button
                    v-if="!item.isSubmitted"
                    class="btn btn-primary"
                    style="padding: 4px 10px; font-size: 12px;"
                    @click="openEvaluationModal(item)"
                  >
                    Nhập phiếu
                  </button>
                  <button
                    v-else
                    class="btn btn-secondary"
                    style="padding: 4px 10px; font-size: 12px;"
                    @click="openViewModal(item)"
                  >
                    Xem phiếu
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- TAB 2: TÔI ĐƯỢC DỰ GIỜ -->
    <div v-if="activeTab === 'TEACHER'">
      <div v-if="loading" class="card" style="text-align: center; padding: 40px; color: var(--text-muted);">
        Đang tải lịch dự giờ...
      </div>
      <div v-else-if="asTeacherList.length === 0" class="empty-state card">
        <div class="empty-icon">🎯</div>
        <div class="empty-title">Không tìm thấy buổi dự giờ</div>
        <div style="font-size: 13px;">Bạn chưa có buổi dạy nào được phân công dự giờ trong kỳ này.</div>
      </div>
      <div v-else class="card" style="padding: 0; overflow: hidden;">
        <div style="padding: 14px 20px; background: #FFF; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between;">
          <h2 class="card-title" style="margin-bottom: 0;">
            Danh sách buổi được dự giờ
          </h2>
          <span class="badge badge-gray">{{ asTeacherList.length }} buổi</span>
        </div>

        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 50px;">STT</th>
                <th>Mã kế hoạch</th>
                <th>Môn học & Lớp dạy</th>
                <th>Thời gian & Phòng</th>
                <th>Người dự giờ phân công</th>
                <th style="text-align: center;">Kết quả đánh giá</th>
                <th style="text-align: center;">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in asTeacherList" :key="item.id">
                <td class="text-center font-mono">{{ idx + 1 }}</td>
                <td style="font-weight: 600;">
                  <span class="badge badge-gray">{{ item.planCode }}</span>
                </td>
                <td>
                  <div style="font-weight: 600; color: var(--text-main); margin-bottom: 4px;">{{ item.subjectName }}</div>
                  <div style="display: flex; gap: 6px; font-size: 11px;">
                    <span class="badge badge-blue">{{ item.subjectCode }}</span>
                    <span class="badge badge-gray">{{ item.classCode }}</span>
                  </div>
                </td>
                <td>
                  <div style="font-weight: 600;">{{ formatDate(item.plannedDate) }}</div>
                  <div style="font-size: 12px; color: var(--text-muted);">Ca {{ item.shift }} &bull; P.{{ item.room || 'F201' }}</div>
                </td>
                <td>
                  <div style="display: flex; flex-direction: column; gap: 4px;">
                    <div v-if="item.inspector1" style="font-size: 12px; display: flex; align-items: center; gap: 6px;">
                      <span style="font-weight: 600; color: var(--text-muted); font-size: 11px;">GV1:</span>
                      <span>{{ item.inspector1.fullName }}</span>
                      <span :style="{ color: item.inspector1Submitted ? 'var(--success)' : 'var(--text-muted)', fontWeight: item.inspector1Submitted ? '600' : '400', fontSize: '11px' }">
                        {{ item.inspector1Submitted ? '(✓ đã nộp)' : '(chưa nộp)' }}
                      </span>
                    </div>
                    <div v-if="item.inspector2" style="font-size: 12px; display: flex; align-items: center; gap: 6px;">
                      <span style="font-weight: 600; color: var(--text-muted); font-size: 11px;">GV2:</span>
                      <span>{{ item.inspector2.fullName }}</span>
                      <span :style="{ color: item.inspector2Submitted ? 'var(--success)' : 'var(--text-muted)', fontWeight: item.inspector2Submitted ? '600' : '400', fontSize: '11px' }">
                        {{ item.inspector2Submitted ? '(✓ đã nộp)' : '(chưa nộp)' }}
                      </span>
                    </div>
                    <div v-if="item.inspector3" style="font-size: 12px; display: flex; align-items: center; gap: 6px;">
                      <span style="font-weight: 600; color: var(--text-muted); font-size: 11px;">GV3:</span>
                      <span>{{ item.inspector3.fullName }}</span>
                      <span :style="{ color: item.inspector3Submitted ? 'var(--success)' : 'var(--text-muted)', fontWeight: item.inspector3Submitted ? '600' : '400', fontSize: '11px' }">
                        {{ item.inspector3Submitted ? '(✓ đã nộp)' : '(chưa nộp)' }}
                      </span>
                    </div>
                  </div>
                </td>
                <td style="text-align: center;">
                  <template v-if="item.hasEvaluations">
                    <div style="font-weight: 700; font-size: 14px;">{{ getAverageScore(item.evaluations) }}/92đ</div>
                    <span
                      class="badge"
                      :class="getAveragePercent(item.evaluations) >= 75 ? 'badge-green' : 'badge-red'"
                      style="margin-top: 2px;"
                    >
                      {{ getAveragePercent(item.evaluations) >= 75 ? 'ĐẠT (' + getAveragePercent(item.evaluations) + '%)' : 'CHƯA ĐẠT (' + getAveragePercent(item.evaluations) + '%)' }}
                    </span>
                  </template>
                  <template v-else>
                    <span class="badge badge-gray">Chờ đánh giá</span>
                  </template>
                </td>
                <td style="text-align: center;">
                  <button
                    class="btn btn-secondary"
                    style="padding: 4px 10px; font-size: 12px;"
                    :disabled="!item.hasEvaluations"
                    @click="openViewModal(item)"
                  >
                    Xem phiếu ({{ item.evaluations?.length || 0 }})
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- MODAL 1: NHẬP PHIẾU ĐÁNH GIÁ (UC-16) -->
    <div v-if="showEvaluationModal" class="modal-overlay" @click.self="closeEvaluationModal">
      <div class="modal-content" style="max-width: 900px;">
        <div class="modal-header">
          <div>
            <div class="modal-title">Phiếu đánh giá giờ giảng (UC-16)</div>
            <div style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">
              GV: <strong>{{ activeSession?.teacherName }}</strong> ({{ activeSession?.teacherAccount }}) &bull;
              Môn: <strong>{{ activeSession?.subjectName }} ({{ activeSession?.subjectCode }})</strong> &bull;
              Lớp: <strong>{{ activeSession?.classCode }}</strong>
            </div>
          </div>
          <button class="btn-icon" @click="closeEvaluationModal">✕</button>
        </div>

        <div class="modal-body">
          <!-- Session Details strip -->
          <div class="banner-info" style="font-size: 12px; display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 8px; margin-bottom: 14px;">
            <div>Ngày: <strong>{{ formatDate(activeSession?.plannedDate) }} (Ca {{ activeSession?.shift }})</strong></div>
            <div>Phòng: <strong>{{ activeSession?.room || 'F201' }}</strong></div>
            <div>Người chấm: <strong>{{ currentUserName }} ({{ activeSession?.myRole }})</strong></div>
            <div>Quy chuẩn: <strong>23 tiêu chí, tối đa 92đ, đạt ≥ 69đ (75%)</strong></div>
          </div>

          <!-- Quick Actions Bar -->
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; padding: 10px 14px; background: var(--gray-bg); border-radius: var(--radius); flex-wrap: gap: 8px;">
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
                        :name="c.id"
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
    <div v-if="showViewModal" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content" style="max-width: 900px;">
        <div class="modal-header">
          <div>
            <div class="modal-title">Chi tiết phiếu đánh giá dự giờ (UC-17)</div>
            <div style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">
              Buổi dự: <strong>{{ activeSession?.classCode }} - {{ activeSession?.subjectName }}</strong> &bull;
              GV dạy: <strong>{{ activeSession?.teacherName }}</strong>
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
            <!-- Inspector selector tabs if multiple evaluations -->
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
