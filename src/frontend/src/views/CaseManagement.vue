<template>
  <Layout title="病例管理" current-page="case">
    <div class="card">
      <div class="search-container">
        <input type="text" class="search-input" placeholder="请输入病例编号" v-model="caseIdQuery">
        <input type="text" class="search-input" placeholder="请输入患者姓名" v-model="patientNameQuery" style="margin-left: 10px;">
        <button class="search-btn" @click="search">搜索</button>
        <button
          v-if="userStore.isDoctor()"
          type="button"
          class="btn btn-primary"
          style="margin-left: 10px;"
          @click="$router.push('/case-create')"
        >创建病例</button>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>病例列表</h2>
      </div>
      <div class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>病例编号</th>
              <th>患者姓名</th>
              <th>创建时间</th>
              <th>影像数量</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="caseItem in cases" :key="caseItem.id">
              <td>{{ caseItem.caseId }}</td>
              <td>{{ caseItem.patientName }}</td>
              <td>{{ caseItem.createTime }}</td>
              <td>{{ caseItem.imageCount }}</td>
              <td>{{ caseItem.status }}</td>
              <td>
                <button class="btn btn-primary" @click="$router.push(`/case-detail/${caseItem.id}`)">查看</button>
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
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import { getCases } from '@/api/cases'
import { toast } from '@/utils/toast'
import userStore from '@/store/userStore'

export default {
  name: 'CaseManagement',
  components: { Layout },
  data() {
    return {
      userStore,
      caseIdQuery: '',
      patientNameQuery: '',
      page: 1,
      pageSize: 10,
      total: 0,
      cases: []
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  mounted() {
    this.fetchCases()
    window.addEventListener('focus', this.handleWindowFocus)
    window.addEventListener('pageshow', this.handlePageShow)
  },
  beforeUnmount() {
    window.removeEventListener('focus', this.handleWindowFocus)
    window.removeEventListener('pageshow', this.handlePageShow)
  },
  beforeRouteEnter(to, from, next) {
    next((vm) => vm.fetchCases())
  },
  methods: {
    async fetchCases() {
      try {
        const caseId = this.caseIdQuery.trim() || undefined
        const patientName = this.patientNameQuery.trim() || undefined

        const data = await getCases({
          page: this.page,
          page_size: this.pageSize,
          case_id: caseId,
          patient_name: patientName
        })

        const list = data?.list || data?.items || []
        this.total = Number(data?.total ?? 0)
        this.cases = list.map((item) => ({
          id: item.id,
          caseId: item.caseId,
          patientName: item.patientName,
          createTime: this.formatDateTime(item.createdAt),
          imageCount: item.imageCount,
          status: item.status
        }))
        if (this.page > this.totalPages && this.totalPages >= 1) {
          this.page = this.totalPages
          return this.fetchCases()
        }
      } catch (err) {
        toast.error(err?.message || '获取病例列表失败')
      }
    },
    handleWindowFocus() {
      this.fetchCases()
    },
    handlePageShow() {
      this.fetchCases()
    },
    search() {
      this.page = 1
      return this.fetchCases()
    },
    goPage(p) {
      const next = Number(p)
      if (!Number.isFinite(next) || next < 1 || next > this.totalPages) return
      if (next === this.page) return
      this.page = next
      return this.fetchCases()
    },
    formatDateTime(date) {
      if (!date) return '-'
      // 2026-03-17T09:30:00 -> 2026-03-17 09:30
      const s = String(date)
      if (s.includes('T')) return s.replace('T', ' ').slice(0, 16)
      return s
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
