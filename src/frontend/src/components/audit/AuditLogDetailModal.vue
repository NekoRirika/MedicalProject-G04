<template>
  <Modal v-model:visible="innerVisible" title="审计日志详情" width-preset="xlarge">
    <div v-if="detail" class="audit-detail">
      <section class="audit-section">
        <h4 class="audit-section-title">基本信息</h4>
        <div class="audit-grid">
          <div v-for="item in baseEntries" :key="item.label" class="audit-field">
            <span class="audit-label">{{ item.label }}</span>
            <span class="audit-value" :class="item.valueClass">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section v-if="technicalEntries.length > 0" class="audit-section">
        <h4 class="audit-section-title">技术信息</h4>
        <div class="audit-grid">
          <div v-for="item in technicalEntries" :key="item.label" class="audit-field">
            <span class="audit-label">{{ item.label }}</span>
            <span class="audit-value" :class="item.valueClass">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section class="audit-section">
        <h4 class="audit-section-title">请求参数</h4>
        <div v-if="argumentEntries.length > 0" class="audit-grid">
          <div v-for="item in argumentEntries" :key="item.key" class="audit-field">
            <span class="audit-label">{{ item.label }}</span>
            <span class="audit-value">{{ item.value }}</span>
          </div>
        </div>
        <p v-else class="audit-empty">该条日志没有记录可展示的请求参数。</p>
      </section>

      <section v-if="rawDetailsText" class="audit-section">
        <h4 class="audit-section-title">原始详情</h4>
        <pre class="audit-raw">{{ rawDetailsText }}</pre>
      </section>
    </div>

    <template #footer>
      <button type="button" class="btn btn-secondary" @click="innerVisible = false">关闭</button>
    </template>
  </Modal>
</template>

<script>
import Modal from '@/components/common/Modal.vue'

const PROPERTY_LABELS = {
  username: '用户名',
  password: '密码',
  oldPassword: '旧密码',
  newPassword: '新密码',
  confirmPassword: '确认密码',
  patientName: '患者姓名',
  patientAge: '患者年龄',
  patientGender: '患者性别',
  patientIdCard: '身份证号',
  patientPhone: '手机号',
  patientBirthday: '出生日期',
  caseDescription: '病例描述',
  checkDate: '检查日期',
  checkNote: '检查备注',
  caseId: '病例ID',
  imageId: '影像ID',
  modelId: '模型ID',
  detectionId: '检测ID',
  evaluation: '评价结果',
  feedback: '反馈意见',
  status: '状态',
}

const METHOD_ARGUMENT_LABELS = {
  getCases: {
    arg0: '页码',
    arg1: '每页数量',
    arg2: '病例编号',
    arg3: '患者姓名',
  },
  getCaseById: {
    arg0: '病例ID',
  },
  createCase: {
    'arg0.patientName': '患者姓名',
    'arg0.patientAge': '患者年龄',
    'arg0.patientGender': '患者性别',
    'arg0.patientIdCard': '身份证号',
    'arg0.patientPhone': '手机号',
    'arg0.patientBirthday': '出生日期',
    'arg0.caseDescription': '病例描述',
    'arg0.checkDate': '检查日期',
    'arg0.checkNote': '检查备注',
  },
  updateCase: {
    arg0: '病例ID',
    'arg1.patientName': '患者姓名',
    'arg1.patientAge': '患者年龄',
    'arg1.patientGender': '患者性别',
    'arg1.caseDescription': '病例描述',
    'arg1.checkDate': '检查日期',
    'arg1.checkNote': '检查备注',
  },
  deleteCase: {
    arg0: '病例ID',
  },
  getImagesByCaseId: {
    arg0: '病例ID',
  },
  uploadImage: {
    arg0: '病例ID',
    arg1: '上传文件',
  },
  deleteImage: {
    arg0: '病例ID',
    arg1: '影像ID',
  },
  createDetection: {
    arg0: '病例ID',
    'arg1.modelId': '模型ID',
    'arg1.imageId': '影像ID',
  },
  getDetectionById: {
    arg0: '病例ID',
    arg1: '检测ID',
  },
  exportDetectionResult: {
    arg0: '病例ID',
    arg1: '检测ID',
    arg2: '导出格式',
  },
  submitFeedback: {
    arg0: '病例ID',
    arg1: '检测ID',
    'arg2.evaluation': '评价结果',
    'arg2.feedback': '反馈意见',
  },
  getUsers: {
    arg0: '页码',
    arg1: '每页数量',
  },
  createUser: {
    'arg0.username': '用户名',
    'arg0.password': '密码',
    'arg0.name': '姓名',
    'arg0.department': '部门',
    'arg0.role': '角色',
  },
  updateUser: {
    arg0: '用户ID',
    'arg1.name': '姓名',
    'arg1.department': '部门',
    'arg1.role': '角色',
  },
  deleteUser: {
    arg0: '用户ID',
  },
  resetPassword: {
    arg0: '用户ID',
  },
  updateStatus: {
    arg0: '用户ID',
    'arg1.status': '状态',
  },
  login: {
    'arg0.username': '用户名',
    'arg0.password': '密码',
  },
}

export default {
  name: 'AuditLogDetailModal',
  components: { Modal },
  props: {
    visible: { type: Boolean, default: false },
    detail: { type: Object, default: null },
  },
  emits: ['update:visible'],
  computed: {
    innerVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      },
    },
    parsedDetails() {
      const raw = this.detail?.details
      if (!raw) return null
      if (typeof raw === 'object') return raw
      try {
        return JSON.parse(raw)
      } catch {
        return null
      }
    },
    rawDetailsText() {
      const raw = this.detail?.details
      if (!raw) return ''
      if (typeof raw === 'string') return raw
      try {
        return JSON.stringify(raw, null, 2)
      } catch {
        return String(raw)
      }
    },
    baseEntries() {
      if (!this.detail) return []
      return [
        { label: '操作时间', value: this.formatDateTime(this.detail.operationTime) },
        { label: '操作人', value: this.detail.operator || '—' },
        { label: '操作类型', value: this.detail.operationType || '—' },
        { label: '操作内容', value: this.detail.operationContent || '—' },
        { label: 'IP 地址', value: this.detail.ipAddress || '—' },
        {
          label: '执行状态',
          value: this.statusLabel(this.detail.status),
          valueClass: this.statusClass(this.detail.status),
        },
      ]
    },
    technicalEntries() {
      const parsed = this.parsedDetails
      if (!parsed) return []
      const list = []
      if (parsed.className) list.push({ label: '控制器类', value: parsed.className })
      if (parsed.methodName) list.push({ label: '方法名', value: parsed.methodName })
      if (parsed.error) {
        list.push({
          label: '错误信息',
          value: parsed.error,
          valueClass: 'audit-value--error',
        })
      }
      return list
    },
    argumentEntries() {
      const args = this.parsedDetails?.arguments
      if (!args || typeof args !== 'object') return []
      const entries = []
      Object.entries(args).forEach(([key, value]) => {
        this.pushArgumentEntries(entries, key, value)
      })
      return entries
    },
  },
  methods: {
    formatDateTime(value) {
      if (!value) return '—'
      if (Array.isArray(value)) {
        return this.formatArrayDate(value)
      }
      const text = String(value)
      return text.includes('T') ? text.replace('T', ' ').slice(0, 19) : text
    },
    formatArrayDate(arr) {
      if (!Array.isArray(arr) || arr.length < 3) return String(arr)
      const pad = (n) => String(n).padStart(2, '0')
      const [y, m, d, hh = null, mm = null, ss = null] = arr
      if (hh === null || mm === null) {
        return `${y}-${pad(m)}-${pad(d)}`
      }
      return `${y}-${pad(m)}-${pad(d)} ${pad(hh)}:${pad(mm)}:${pad(ss ?? 0)}`
    },
    statusLabel(status) {
      const value = String(status || '').toLowerCase()
      if (value === 'success') return '成功'
      if (value === 'failed') return '失败'
      return status || '—'
    },
    statusClass(status) {
      const value = String(status || '').toLowerCase()
      if (value === 'success') return 'audit-value--success'
      if (value === 'failed') return 'audit-value--error'
      return ''
    },
    pushArgumentEntries(entries, key, value, parentPath = '') {
      const path = parentPath ? `${parentPath}.${key}` : key
      if (value === undefined) return

      if (Array.isArray(value)) {
        entries.push({
          key: path,
          label: this.argumentLabel(path),
          value: this.formatArrayOrList(value),
        })
        return
      }

      if (value && typeof value === 'object') {
        Object.entries(value).forEach(([childKey, childValue]) => {
          this.pushArgumentEntries(entries, childKey, childValue, path)
        })
        return
      }

      entries.push({
        key: path,
        label: this.argumentLabel(path),
        value: this.formatScalarValue(path, value),
      })
    },
    argumentLabel(path) {
      const methodName = this.parsedDetails?.methodName
      const methodMap = METHOD_ARGUMENT_LABELS[methodName] || {}
      if (methodMap[path]) return methodMap[path]

      const leaf = path.split('.').pop()
      if (PROPERTY_LABELS[leaf]) return PROPERTY_LABELS[leaf]

      if (/^arg\d+$/.test(path)) {
        return `参数 ${Number(path.replace('arg', '')) + 1}`
      }
      return path
    },
    formatArrayOrList(value) {
      if (value.every((item) => typeof item === 'number') && value.length >= 3 && value.length <= 6) {
        return this.formatArrayDate(value)
      }
      return value.map((item) => this.formatScalarValue('', item)).join(' / ')
    },
    formatScalarValue(path, value) {
      if (value === null || value === '') return '—'
      const lowerPath = String(path).toLowerCase()
      if (lowerPath.includes('password')) return '••••••••'
      if (Array.isArray(value)) return this.formatArrayOrList(value)
      if (value && typeof value === 'object') {
        try {
          return JSON.stringify(value)
        } catch {
          return String(value)
        }
      }
      return String(value)
    },
  },
}
</script>

<style scoped>
.audit-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.audit-section {
  border: 1px solid #ebeff5;
  border-radius: 10px;
  background: #fbfcfe;
  padding: 16px;
}

.audit-section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #263238;
}

.audit-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px 16px;
}

.audit-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.audit-label {
  font-size: 12px;
  color: #73808c;
}

.audit-value {
  color: #24313d;
  line-height: 1.5;
  word-break: break-word;
}

.audit-value--success {
  color: #1b8a44;
  font-weight: 600;
}

.audit-value--error {
  color: #c0392b;
  font-weight: 600;
}

.audit-empty {
  margin: 0;
  color: #73808c;
  font-size: 13px;
}

.audit-raw {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #111827;
  color: #e5eef7;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
}
</style>
