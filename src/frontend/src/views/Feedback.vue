<template>
  <Layout title="评价反馈" current-page="feedback">
    <div class="card">
      <div class="search-container">
        <input
          type="text"
          class="search-input"
          placeholder="筛选当前页：病例编号 / 患者姓名 / 评价 / 操作人"
          v-model="searchQuery"
        >
        <button type="button" class="search-btn" @click="applyLocalFilter">筛选</button>
        <button type="button" class="btn btn-secondary" style="margin-left: 8px;" @click="resetSearch">重置</button>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>评价记录</h2>
      </div>
      <div v-if="loading" class="feedback-loading">加载中…</div>
      <template v-else>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>病例编号</th>
                <th>患者姓名</th>
                <th>检测时间</th>
                <th>评价结果</th>
                <th>反馈时间</th>
                <th>操作人</th>
                <th>反馈内容</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in displayedRecords" :key="record.id">
                <td>{{ record.caseNo || '—' }}</td>
                <td>{{ record.patientName || '—' }}</td>
                <td>{{ formatDateTime(record.detectTime) }}</td>
                <td>{{ record.evaluation || '—' }}</td>
                <td>{{ formatDateTime(record.feedbackTime) }}</td>
                <td>{{ record.operator || '—' }}</td>
                <td class="feedback-text-cell" :title="record.feedback">{{ truncate(record.feedback, 40) }}</td>
                <td>
                  <button type="button" class="btn btn-primary btn-sm" @click="openDetail(record)">查看</button>
                  <button
                    v-if="canOpenDetection(record)"
                    type="button"
                    class="btn btn-secondary btn-sm"
                    style="margin-left: 6px;"
                    @click="goDetectionResult(record)"
                  >
                    检测结果
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="!loading && displayedRecords.length === 0" class="feedback-empty">
          {{ records.length === 0 ? '暂无评价反馈记录。' : '当前页无匹配筛选条件的记录。' }}
        </p>
        <div v-if="total > 0" class="pagination">
          <span class="pagination-meta">共 {{ total }} 条，每页 {{ pageSize }} 条</span>
          <div class="pagination-btns">
            <button type="button" class="pg-btn" :disabled="page <= 1" @click="goPage(1)">首页</button>
            <button type="button" class="pg-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
            <span class="pagination-current">第 {{ page }} / {{ totalPages }} 页</span>
            <button type="button" class="pg-btn" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
            <button type="button" class="pg-btn" :disabled="page >= totalPages" @click="goPage(totalPages)">末页</button>
          </div>
        </div>
      </template>
    </div>

    <Modal v-model:visible="detailVisible" title="反馈详情" width-preset="medium">
      <div v-if="detailRecord" class="detail-body">
        <div class="detail-row"><span class="detail-k">病例编号</span><span class="detail-v">{{ detailRecord.caseNo || '—' }}</span></div>
        <div class="detail-row"><span class="detail-k">患者姓名</span><span class="detail-v">{{ detailRecord.patientName || '—' }}</span></div>
        <div class="detail-row"><span class="detail-k">检测时间</span><span class="detail-v">{{ formatDateTime(detailRecord.detectTime) }}</span></div>
        <div class="detail-row"><span class="detail-k">评价结果</span><span class="detail-v">{{ detailRecord.evaluation || '—' }}</span></div>
        <div class="detail-row"><span class="detail-k">反馈时间</span><span class="detail-v">{{ formatDateTime(detailRecord.feedbackTime) }}</span></div>
        <div class="detail-row"><span class="detail-k">操作人</span><span class="detail-v">{{ detailRecord.operator || '—' }}</span></div>
        <div class="detail-row detail-row--block">
          <span class="detail-k">反馈内容</span>
          <div class="detail-feedback">{{ detailRecord.feedback || '—' }}</div>
        </div>
      </div>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="detailVisible = false">关闭</button>
        <button
          v-if="detailRecord && canOpenDetection(detailRecord)"
          type="button"
          class="btn btn-primary"
          @click="goDetectionResult(detailRecord); detailVisible = false"
        >
          打开检测结果
        </button>
      </template>
    </Modal>
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import Modal from '@/components/common/Modal.vue'
import { getFeedbacks } from '@/api/feedbacks'
import { toast } from '@/utils/toast'
import userStore from '@/store/userStore'

export default {
  name: 'Feedback',
  components: { Layout, Modal },
  data() {
    return {
      userStore,
      searchQuery: '',
      filterKeyword: '',
      page: 1,
      pageSize: 10,
      total: 0,
      records: [],
      loading: false,
      detailVisible: false,
      detailRecord: null
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    displayedRecords() {
      const kw = this.filterKeyword.trim().toLowerCase()
      if (!kw) return this.records
      return this.records.filter((r) => {
        const parts = [
          r.caseNo,
          r.patientName,
          r.evaluation,
          r.operator,
          r.feedback
        ]
          .filter(Boolean)
          .map((s) => String(s).toLowerCase())
        return parts.some((p) => p.includes(kw))
      })
    }
  },
  mounted() {
    this.fetchFeedbacks()
  },
  methods: {
    async fetchFeedbacks() {
      this.loading = true
      try {
        const data = await getFeedbacks({
          page: this.page,
          pageSize: this.pageSize
        })
        const list = data?.data ?? data?.list ?? []
        this.total = Number(data?.total ?? 0)
        this.records = Array.isArray(list) ? list : []
        if (this.page > this.totalPages && this.totalPages >= 1) {
          this.page = this.totalPages
          return this.fetchFeedbacks()
        }
      } catch (err) {
        toast.error(err?.message || '获取评价反馈列表失败')
        this.records = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    applyLocalFilter() {
      this.filterKeyword = this.searchQuery
    },
    resetSearch() {
      this.searchQuery = ''
      this.filterKeyword = ''
    },
    goPage(p) {
      const next = Number(p)
      if (!Number.isFinite(next) || next < 1 || next > this.totalPages) return
      if (next === this.page) return
      this.page = next
      this.fetchFeedbacks()
    },
    formatDateTime(v) {
      if (v == null || v === '') return '—'
      if (typeof v === 'string') {
        const s = v
        if (s.includes('T')) return s.replace('T', ' ').slice(0, 19)
        return s
      }
      if (Array.isArray(v) && v.length >= 3) {
        const [y, m, d, h = 0, min = 0, sec = 0] = v
        const pad = (n) => String(n).padStart(2, '0')
        if (v.length >= 6) {
          return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(min)}:${pad(sec)}`.slice(0, 19)
        }
        return `${y}-${pad(m)}-${pad(d)}`
      }
      return String(v)
    },
    truncate(s, max) {
      if (s == null || s === '') return '—'
      const t = String(s)
      return t.length <= max ? t : `${t.slice(0, max)}…`
    },
    openDetail(row) {
      this.detailRecord = row
      this.detailVisible = true
    },
    /**
     * 接口 v1.1 示例未包含病例主键、检测 ID；若后端在列表项中增加 numeric `caseId`（病例 id）与 `detectionId`，
     * 医生可跳转至检测结果页。当前无字段时按钮不显示。
     */
    canOpenDetection(record) {
      if (!userStore.isDoctor()) return false
      const cid = record?.caseId ?? record?.case_id
      const did = record?.detectionId ?? record?.detection_id
      return cid != null && did != null && String(cid) !== '' && String(did) !== ''
    },
    goDetectionResult(record) {
      const cid = record?.caseId ?? record?.case_id
      const did = record?.detectionId ?? record?.detection_id
      if (cid == null || did == null) return
      this.$router.push(`/detection-result/${Number(cid)}/${Number(did)}`)
    }
  }
}
</script>

<style scoped>
.search-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #757575;
  line-height: 1.4;
}

.feedback-loading {
  padding: 24px;
  text-align: center;
  color: var(--text-light, #666);
}

.feedback-empty {
  padding: 16px;
  text-align: center;
  color: var(--text-light, #666);
  font-size: 14px;
}

.feedback-text-cell {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-sm {
  padding: 4px 10px;
  font-size: 12px;
}

.pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0 4px;
  border-top: 1px solid #eee;
  margin-top: 8px;
}

.pagination-meta {
  font-size: 13px;
  color: #616161;
}

.pagination-btns {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pg-btn {
  padding: 6px 12px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.pg-btn:hover:not(:disabled) {
  background: #f5f5f5;
}

.pg-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pagination-current {
  font-size: 13px;
  color: #333;
  padding: 0 6px;
}

.detail-body {
  font-size: 14px;
}

.detail-row {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  align-items: flex-start;
}

.detail-row--block {
  flex-direction: column;
  gap: 6px;
}

.detail-k {
  flex: 0 0 88px;
  color: #757575;
}

.detail-v {
  flex: 1;
  word-break: break-word;
}

.detail-feedback {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 6px;
  border: 1px solid #e8eef8;
}
</style>
