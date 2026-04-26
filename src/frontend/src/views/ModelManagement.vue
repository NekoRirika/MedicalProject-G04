<template>
  <Layout title="模型管理" current-page="model">
    <div class="card">
      <div class="card-header">
        <h2>模型列表</h2>
      </div>
      <div class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>模型名称</th>
              <th>版本</th>
              <th>状态</th>
              <th>准确率</th>
              <th>加载时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="model in models" :key="model.id">
              <td>{{ model.name }}</td>
              <td>{{ model.version }}</td>
              <td>{{ model.statusLabel }}</td>
              <td>{{ model.accuracy }}%</td>
              <td>{{ model.loadTime }}</td>
              <td>
                <div class="btn-group">
                  <button
                    :class="model.isCurrent ? 'btn btn-secondary' : 'btn btn-primary'"
                    :disabled="model.isCurrent"
                    @click="switchModel(model.id)"
                  >
                    {{ model.isCurrent ? '当前模型' : '切换' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>当前模型</h2>
      </div>
      <div style="padding: 20px;">
        <div class="form-row">
          <div class="form-group">
            <label>模型名称</label>
            <input type="text" :value="currentModel.name + ' ' + currentModel.version" disabled>
          </div>
          <div class="form-group">
            <label>状态</label>
            <input type="text" value="当前使用中" disabled>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>准确率</label>
            <input type="text" :value="currentModel.accuracy + '%'" disabled>
          </div>
          <div class="form-group">
            <label>切换时间</label>
            <input type="text" :value="currentModel.activatedAtDisplay" disabled>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import { getModels, activateModel } from '@/api/models'

export default {
  name: 'ModelManagement',
  components: { Layout },
  data() {
    return {
      models: []
    }
  },
  computed: {
    currentModel() {
      return this.models.find((m) => m.isCurrent) || this.models[0] || {}
    }
  },
  mounted() {
    this.fetchModels()
  },
  methods: {
    formatDateTime(date) {
      if (!date) return '-'
      const s = String(date)
      if (s.includes('T')) return s.replace('T', ' ').slice(0, 19)
      return s
    },
    fetchModels() {
      return getModels().then((data) => {
        const items = data || []
        this.models = items.map((m) => {
          const isCurrent = m.status === 'active'
          const accuracyValue = Number(m.accuracy)
          const accuracyPercent = accuracyValue <= 1 ? accuracyValue * 100 : accuracyValue
          let version = m.version
          if (!version.startsWith('v')) {
            version = 'v' + version
          }
          return {
            id: m.id,
            name: m.name,
            version: version,
            statusLabel: isCurrent ? '使用中' : '待切换',
            accuracy: Number(accuracyPercent.toFixed(2)),
            loadTime: this.formatDateTime(m.loadedAt),
            activatedAtDisplay: this.formatDateTime(m.activatedAt),
            isCurrent
          }
        })
      })
    },
    async switchModel(modelId) {
      try {
        await activateModel(modelId)
        await this.fetchModels()
      } catch (error) {
        console.error('切换模型失败:', error)
        alert('切换模型失败：' + (error.message || '服务器内部错误，请稍后重试'))
      }
    }
  }
}
</script>

<style scoped>
.btn-group {
  display: flex;
  gap: 8px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 4px;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
