<template>
  <Layout title="创建病例" current-page="case">
    <div class="card">
      <div class="card-header">
        <h2>病例基础信息</h2>
      </div>
      <form @submit.prevent="submitForm" class="form-container">
        <div class="form-row">
          <div class="form-group">
            <label for="patient-name">患者姓名 *</label>
            <input type="text" id="patient-name" v-model="form.patientName" name="patient-name" placeholder="请输入患者姓名（脱敏处理）" required>
          </div>
          <div class="form-group">
            <label for="patient-gender">患者性别</label>
            <select id="patient-gender" v-model="form.patientGender" name="patient-gender">
              <option value="">请选择</option>
              <option value="男">男</option>
              <option value="女">女</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label for="patient-id-card">患者身份证号</label>
            <input type="text" id="patient-id-card" v-model="form.patientIdCard" name="patient-id-card" placeholder="请输入患者身份证号" maxlength="18" @blur="validateIdCard">
            <span v-if="errors.patientIdCard" class="error-text">{{ errors.patientIdCard }}</span>
          </div>
          <div class="form-group">
            <label for="patient-phone">患者手机号</label>
            <input type="text" id="patient-phone" v-model="form.patientPhone" name="patient-phone" placeholder="请输入患者手机号" maxlength="11" @blur="validatePhone">
            <span v-if="errors.patientPhone" class="error-text">{{ errors.patientPhone }}</span>
          </div>
          <div class="form-group">
            <label for="patient-birthday">患者出生日期</label>
            <input type="date" id="patient-birthday" v-model="form.patientBirthday" name="patient-birthday" :max="maxDate">
            <span v-if="errors.patientBirthday" class="error-text">{{ errors.patientBirthday }}</span>
          </div>
        </div>
        <div class="form-group">
          <label for="case-description">病例描述 *</label>
          <textarea id="case-description" v-model="form.caseDescription" name="case-description" rows="4" placeholder="请输入病例描述" required></textarea>
        </div>
        <div class="form-group">
          <label for="check-date">检查日期</label>
          <input type="date" id="check-date" v-model="form.checkDate" name="check-date">
        </div>
        <div class="form-group">
          <label for="check-note">检查备注</label>
          <textarea id="check-note" v-model="form.checkNote" name="check-note" rows="2" placeholder="请输入检查备注"></textarea>
        </div>
        <div class="btn-group">
          <button type="submit" class="btn btn-primary">提交</button>
          <button type="button" class="btn btn-secondary" @click="cancel">取消</button>
        </div>
      </form>
    </div>
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import { createCase } from '@/api/cases'
import { toast } from '@/utils/toast'

export default {
  name: 'CaseCreate',
  components: { Layout },
  data() {
    return {
      form: {
        patientName: '',
        patientGender: '',
        patientIdCard: '',
        patientPhone: '',
        patientBirthday: '',
        caseDescription: '',
        checkDate: '',
        checkNote: ''
      },
      errors: {
        patientIdCard: '',
        patientPhone: '',
        patientBirthday: ''
      }
    }
  },
  computed: {
    maxDate() {
      const today = new Date()
      const year = today.getFullYear()
      const month = String(today.getMonth() + 1).padStart(2, '0')
      const day = String(today.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  },
  methods: {
    validateIdCard() {
      if (!this.form.patientIdCard) {
        this.errors.patientIdCard = ''
        return true
      }
      const idCardRegex = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/
      if (!idCardRegex.test(this.form.patientIdCard)) {
        this.errors.patientIdCard = '请输入有效的18位身份证号码'
        return false
      }
      this.errors.patientIdCard = ''
      return true
    },
    validatePhone() {
      if (!this.form.patientPhone) {
        this.errors.patientPhone = ''
        return true
      }
      const phoneRegex = /^1[3-9]\d{9}$/
      if (!phoneRegex.test(this.form.patientPhone)) {
        this.errors.patientPhone = '请输入有效的11位手机号码'
        return false
      }
      this.errors.patientPhone = ''
      return true
    },
    validateForm() {
      let isValid = true

      if (this.form.patientIdCard && !this.validateIdCard()) {
        isValid = false
      }
      if (this.form.patientPhone && !this.validatePhone()) {
        isValid = false
      }
      if (this.form.patientBirthday) {
        const birthday = new Date(this.form.patientBirthday)
        const today = new Date()
        today.setHours(0, 0, 0, 0)
        if (birthday > today) {
          this.errors.patientBirthday = '出生日期不能晚于今天'
          isValid = false
        } else {
          this.errors.patientBirthday = ''
        }
      }

      return isValid
    },
    async submitForm() {
      if (!this.validateForm()) {
        toast.warning('请检查表单填写是否正确')
        return
      }

      const payload = {
        patientName: this.form.patientName,
        patientGender: this.form.patientGender,
        patientIdCard: this.form.patientIdCard,
        patientPhone: this.form.patientPhone,
        patientBirthday: this.form.patientBirthday || undefined,
        caseDescription: this.form.caseDescription,
        checkDate: this.form.checkDate,
        checkNote: this.form.checkNote
      }

      try {
        await createCase(payload)
        toast.success('创建成功')
        this.$router.push('/case-management')
      } catch (err) {
        toast.error(err.message || '创建失败')
      }
    },
    cancel() {
      this.$router.push('/case-management')
    }
  }
}
</script>

<style scoped>
.error-text {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #dc3545;
}
</style>
