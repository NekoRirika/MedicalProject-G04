<template>
  <Modal v-model:visible="innerVisible" title="编辑病例" width-preset="xlarge">
    <form id="edit-case-form" class="form-container modal-form" @submit.prevent="submit">
      <div class="form-row">
        <div class="form-group">
          <label for="ec-patientName">患者姓名</label>
          <input
            id="ec-patientName"
            v-model="form.patientName"
            type="text"
            placeholder="患者姓名"
            autocomplete="off"
          >
        </div>
        <div class="form-group">
          <label for="ec-patientGender">患者性别</label>
          <select id="ec-patientGender" v-model="form.patientGender">
            <option value="男">男</option>
            <option value="女">女</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label for="ec-checkDate">检查日期</label>
          <input id="ec-checkDate" v-model="form.checkDate" type="date">
        </div>
      </div>
      <div class="form-group">
        <label for="ec-caseDescription">病例描述</label>
        <textarea
          id="ec-caseDescription"
          v-model="form.caseDescription"
          rows="4"
          placeholder="病史与就诊情况描述"
        ></textarea>
      </div>
      <div class="form-group">
        <label for="ec-checkNote">检查备注</label>
        <textarea
          id="ec-checkNote"
          v-model="form.checkNote"
          rows="2"
          placeholder="检查备注"
        ></textarea>
      </div>
    </form>
    <template #footer>
      <button type="button" class="btn btn-secondary" :disabled="saving" @click="close">取消</button>
      <button type="submit" form="edit-case-form" class="btn btn-primary" :disabled="saving">
        {{ saving ? '保存中…' : '保存' }}
      </button>
    </template>
  </Modal>
</template>

<script>
import Modal from '@/components/common/Modal.vue'
import { updateCase } from '@/api/cases'
import { toast } from '@/utils/toast'

const emptyForm = () => ({
  patientName: '',
  patientGender: '男',
  caseDescription: '',
  checkDate: '',
  checkNote: ''
})

export default {
  name: 'EditCaseModal',
  components: { Modal },
  props: {
    visible: { type: Boolean, default: false },
    caseId: { type: Number, required: true },
    caseDetail: { type: Object, default: null }
  },
  emits: ['update:visible', 'saved'],
  data() {
    return {
      form: emptyForm(),
      saving: false
    }
  },
  computed: {
    innerVisible: {
      get() {
        return this.visible
      },
      set(v) {
        this.$emit('update:visible', v)
      }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.syncFromDetail()
      }
    },
    caseDetail: {
      deep: true,
      handler() {
        if (this.visible) this.syncFromDetail()
      }
    }
  },
  methods: {
    syncFromDetail() {
      const d = this.caseDetail
      if (!d) {
        this.form = emptyForm()
        return
      }
      let checkDate = d.checkDate || ''
      if (checkDate && checkDate.includes('T')) {
        checkDate = checkDate.slice(0, 10)
      }
      this.form = {
        patientName: d.patientName ?? '',
        patientGender: d.patientGender === '男' || d.patientGender === '女' ? d.patientGender : '男',
        caseDescription: d.caseDescription ?? '',
        checkDate,
        checkNote: d.checkNote ?? ''
      }
    },
    close() {
      if (this.saving) return
      this.innerVisible = false
    },
    async submit() {
      const { patientName, patientGender, caseDescription, checkDate, checkNote } = this.form
      if (!patientName || String(patientName).trim() === '') {
        toast.warning('请填写患者姓名')
        return
      }

      this.saving = true
      try {
        await updateCase(this.caseId, {
          patientName: String(patientName).trim(),
          patientGender,
          caseDescription: caseDescription != null ? String(caseDescription) : '',
          checkDate: checkDate || '',
          checkNote: checkNote != null ? String(checkNote) : ''
        })
        toast.success('编辑成功')
        this.innerVisible = false
        this.$emit('saved')
      } catch (err) {
        toast.error(err.message || '编辑失败')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>
