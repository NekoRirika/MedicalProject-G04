<template>
  <Layout title="审计日志" current-page="audit">
    <div class="card">
      <div class="card-header">
        <h2>日志筛选</h2>
      </div>
      <div class="form-container">
        <div class="form-row">
          <div class="form-group">
            <label for="operator">操作人</label>
            <input type="text" id="operator" v-model="filters.operator" placeholder="请输入操作人">
          </div>
          <div class="form-group">
            <label for="operation-type">操作类型</label>
            <select id="operation-type" v-model="filters.operationType">
              <option value="">全部</option>
              <option value="LOGIN">登录</option>
              <option value="LOGOUT">登出</option>
              <option value="CREATE_CASE">创建病例</option>
              <option value="QUERY_CASE">查询病例</option>
              <option value="UPDATE_CASE">更新病例</option>
              <option value="DELETE_CASE">删除病例</option>
              <option value="UPLOAD_IMAGE">上传影像</option>
              <option value="QUERY_IMAGE">查询影像</option>
              <option value="DELETE_IMAGE">删除影像</option>
              <option value="CREATE_DETECTION">创建检测</option>
              <option value="QUERY_DETECTION">查询检测</option>
              <option value="EXPORT_DETECTION">导出检测</option>
              <option value="FEEDBACK_DETECTION">检测反馈</option>
              <option value="CREATE_USER">创建用户</option>
              <option value="QUERY_USER">查询用户</option>
              <option value="UPDATE_USER">更新用户</option>
              <option value="DELETE_USER">删除用户</option>
              <option value="RESET_PASSWORD">重置密码</option>
              <option value="UPDATE_STATUS">更新状态</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label for="start-time">开始时间</label>
            <input type="datetime-local" id="start-time" v-model="filters.startTime">
          </div>
          <div class="form-group">
            <label for="end-time">结束时间</label>
            <input type="datetime-local" id="end-time" v-model="filters.endTime">
          </div>
        </div>
        <div class="btn-group">
          <button class="btn btn-primary" @click="search">查询</button>
          <button class="btn btn-secondary" @click="reset">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>操作记录</h2>
      </div>
      <div class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>操作时间</th>
              <th>操作人</th>
              <th>操作类型</th>
              <th>操作内容</th>
              <th>IP地址</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>{{ log.time }}</td>
              <td>{{ log.operator }}</td>
              <td>{{ log.type }}</td>
              <td>{{ log.content }}</td>
              <td>{{ log.ip }}</td>
              <td>{{ log.status }}</td>
              <td>
                <button class="btn btn-primary" @click="viewDetail(log)">查看详情</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
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
    </div>

    <AuditLogDetailModal
      v-model:visible="detailVisible"
      :detail="detailRecord"
    />
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import AuditLogDetailModal from '@/components/audit/AuditLogDetailModal.vue'
import { getAuditLogs, getAuditLogDetail } from '@/api/auditLogs'
import { toast } from '@/utils/toast'

export default {
  name: 'AuditLog',
  components: { Layout, AuditLogDetailModal },
  data() {
    return {
      filters: {
        operator: '',
        operationType: '',
        startTime: '',
        endTime: ''
      },
      page: 1,
      pageSize: 10,
      total: 0,
      logs: [],
      detailVisible: false,
      detailRecord: null
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  mounted() {
    this.search()
  },
  methods: {
    toBackendDateTime(v) {
      if (!v) return undefined
      // datetime-local: 2026-03-17T10:00 -> 后端接受 yyyy-MM-ddTHH:mm:ss 或 yyyy-MM-dd
      return String(v)
    },
    formatDateTime(date) {
      if (!date) return '-'
      const s = String(date)
      if (s.includes('T')) return s.replace('T', ' ').slice(0, 19)
      return s
    },
    async search() {
      const data = await getAuditLogs({
        page: this.page,
        page_size: this.pageSize,
        operator: this.filters.operator || undefined,
        operation_type: this.filters.operationType || undefined,
        start_time: this.filters.startTime || undefined,
        end_time: this.filters.endTime || undefined
      })

      const list = data?.list || data?.items || []
      this.total = Number(data?.total ?? 0)
      this.logs = list.map((log) => ({
        id: log.id,
        time: this.formatDateTime(log.operationTime),
        operator: log.operator,
        type: log.operationType,
        content: log.operationContent,
        ip: log.ipAddress,
        status: log.status
      }))
      if (this.page > this.totalPages && this.totalPages >= 1) {
        this.page = this.totalPages
        return this.search()
      }
    },
    reset() {
      this.filters = { operator: '', operationType: '', startTime: '', endTime: '' }
      this.page = 1
      this.search()
    },
    goPage(p) {
      const next = Number(p)
      if (!Number.isFinite(next) || next < 1 || next > this.totalPages) return
      if (next === this.page) return
      this.page = next
      return this.search()
    },
    async viewDetail(log) {
      try {
        const detail = await getAuditLogDetail(log.id)
        this.detailRecord = detail
        this.detailVisible = true
      } catch (err) {
        toast.error(err.message || '获取详情失败')
      }
    }
  }
}
</script>

<style scoped>
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
</style>
