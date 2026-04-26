<template>
  <Modal v-model:visible="innerVisible" title="修改密码" width-preset="narrow">
    <form class="pwd-form" @submit.prevent="submit">
      <div class="form-group">
        <label for="cp-old">原密码</label>
        <input id="cp-old" v-model="form.oldPassword" type="password" autocomplete="current-password" placeholder="请输入原密码">
      </div>
      <div class="form-group">
        <label for="cp-new">新密码</label>
        <input id="cp-new" v-model="form.newPassword" type="password" autocomplete="new-password" placeholder="请输入新密码" @input="checkPasswordStrength">
        <div class="password-strength" :style="{ color: passwordStrengthColor }">{{ passwordStrengthText }}</div>
      </div>
      <div class="form-group">
        <label for="cp-confirm">确认新密码</label>
        <input id="cp-confirm" v-model="form.confirmPassword" type="password" autocomplete="new-password" placeholder="请再次输入新密码">
      </div>
    </form>
    <template #footer>
      <button type="button" class="btn btn-secondary" @click="close">取消</button>
      <button type="button" class="btn btn-primary" @click="submit">保存</button>
    </template>
  </Modal>
</template>

<script>
import Modal from '@/components/common/Modal.vue'
import { toast } from '@/utils/toast'

export default {
  name: 'ChangePasswordModal',
  components: { Modal },
  props: {
    visible: { type: Boolean, default: false }
  },
  emits: ['update:visible', 'success'],
  data() {
    return {
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordStrengthText: '请输入密码',
      passwordStrengthColor: '#757575'
    }
  },
  computed: {
    innerVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  watch: {
    visible(val) {
      if (!val) this.reset()
    }
  },
  methods: {
    close() {
      this.innerVisible = false
    },
    reset() {
      this.form = { oldPassword: '', newPassword: '', confirmPassword: '' }
      this.passwordStrengthText = '请输入密码'
      this.passwordStrengthColor = '#757575'
    },
    checkPasswordStrength() {
      const pwd = this.form.newPassword
      let strength = 0
      if (pwd.length >= 8) strength++
      if (/[A-Z]/.test(pwd)) strength++
      if (/[a-z]/.test(pwd)) strength++
      if (/[0-9]/.test(pwd)) strength++
      if (/[^A-Za-z0-9]/.test(pwd)) strength++
      switch (strength) {
        case 0:
          this.passwordStrengthText = '请输入密码'
          this.passwordStrengthColor = '#757575'
          break
        case 1:
        case 2:
          this.passwordStrengthText = '密码强度：弱'
          this.passwordStrengthColor = '#f44336'
          break
        case 3:
          this.passwordStrengthText = '密码强度：中'
          this.passwordStrengthColor = '#ff9800'
          break
        default:
          this.passwordStrengthText = '密码强度：强'
          this.passwordStrengthColor = '#4caf50'
      }
    },
    submit() {
      if (this.form.newPassword !== this.form.confirmPassword) {
        toast.warning('两次输入的密码不一致')
        return
      }
      this.$emit('success', { ...this.form })
    }
  }
}
</script>

<style scoped>
.pwd-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  color: #424242;
}

.form-group input {
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.password-strength {
  margin-top: 2px;
  font-size: 12px;
}
</style>
