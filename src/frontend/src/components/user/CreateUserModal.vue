<template>
  <Modal v-model:visible="innerVisible" :title="title" width-preset="wide">
    <form id="user-form" class="form-container modal-form" @submit.prevent="submit">
      <div class="form-row">
        <div class="form-group">
          <label for="username">用户名 *</label>
          <input type="text" id="username" v-model="form.username" placeholder="请输入用户名" required>
        </div>
        <div class="form-group">
          <label for="password">密码 *</label>
          <input type="password" id="password" v-model="form.password" placeholder="请输入密码" required>
          <div class="password-strength">{{ passwordStrengthText }}</div>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label for="name">姓名 *</label>
          <input type="text" id="name" v-model="form.name" placeholder="请输入姓名" required>
        </div>
        <div class="form-group">
          <label for="department">部门 *</label>
          <select id="department" v-model="form.department" @change="onDepartmentChange" required>
            <option value="">请选择部门</option>
            <option value="管理部">管理部</option>
            <option value="科研部">科研部</option>
            <option value="心内科">心内科</option>
            <option value="呼吸科">呼吸科</option>
            <option value="消化科">消化科</option>
            <option value="神经内科">神经内科</option>
            <option value="神经外科">神经外科</option>
            <option value="骨科">骨科</option>
            <option value="普外科">普外科</option>
            <option value="胸外科">胸外科</option>
            <option value="妇产科">妇产科</option>
            <option value="儿科">儿科</option>
            <option value="内分泌科">内分泌科</option>
            <option value="肾内科">肾内科</option>
            <option value="血液科">血液科</option>
            <option value="肿瘤科">肿瘤科</option>
            <option value="感染科">感染科</option>
            <option value="风湿免疫科">风湿免疫科</option>
            <option value="急诊科">急诊科</option>
            <option value="重症医学科">重症医学科</option>
            <option value="放射科">放射科</option>
          </select>
          <div v-if="departmentHint" class="department-hint">{{ departmentHint }}</div>
        </div>
      </div>

      <div class="form-group">
        <label for="role">角色 *</label>
        <select id="role" v-model="form.role" :disabled="roleAutoLocked" required>
          <option value="">请选择</option>
            <option value="admin">管理员</option>
            <option value="doctor">医生</option>
            <option value="researcher">科研人员</option>
        </select>
      </div>
    </form>

    <template #footer>
      <button type="button" class="btn btn-secondary" @click="close">取消</button>
      <button type="button" class="btn btn-primary" @click="submit">提交</button>
    </template>
  </Modal>
</template>

<script>
import Modal from '@/components/common/Modal.vue'
import { toast } from '@/utils/toast'

const DOCTOR_DEPARTMENTS = [
  '心内科', '呼吸科', '消化科', '神经内科', '神经外科',
  '骨科', '普外科', '胸外科', '妇产科', '儿科',
  '内分泌科', '肾内科', '血液科', '肿瘤科', '感染科',
  '风湿免疫科', '急诊科', '重症医学科', '放射科'
]

export default {
  name: 'CreateUserModal',
  components: { Modal },
  props: {
    visible: { type: Boolean, default: false },
    title: { type: String, default: '创建账号' }
  },
  emits: ['update:visible', 'created'],
  data() {
    return {
      form: {
        username: '',
        password: '',
        name: '',
        department: '',
        role: ''
      },
      roleAutoLocked: false,
      departmentHint: ''
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
    },
    passwordStrengthText() {
      const len = this.form.password.length
      if (!len) return '请输入密码'
      if (len < 6) return '密码强度：弱'
      if (len < 10) return '密码强度：中'
      return '密码强度：强'
    }
  },
  watch: {
    visible(val) {
      if (!val) this.resetForm()
    }
  },
  methods: {
    onDepartmentChange() {
      const dept = this.form.department
      this.roleAutoLocked = false
      this.departmentHint = ''

      if (dept === '管理部') {
        this.form.role = 'admin'
        this.roleAutoLocked = true
        this.departmentHint = '管理部只能创建管理员账号'
      } else if (dept === '科研部') {
        this.form.role = 'researcher'
        this.roleAutoLocked = true
        this.departmentHint = '科研部只能创建科研人员账号'
      } else if (DOCTOR_DEPARTMENTS.includes(dept)) {
        this.form.role = 'doctor'
        this.roleAutoLocked = true
        this.departmentHint = '医生部门只能创建医生账号'
      } else {
        this.form.role = ''
      }
    },
    close() {
      this.innerVisible = false
    },
    resetForm() {
      this.form = {
        username: '',
        password: '',
        name: '',
        department: '',
        role: ''
      }
      this.roleAutoLocked = false
      this.departmentHint = ''
    },
    submit() {
      const { username, password, name, department, role } = this.form
      if (!username || !password || !name || !department || !role) {
        toast.warning('请填写完整的账号信息')
        return
      }

      this.$emit('created', {
        username,
        password,
        name,
        department,
        role
      })
      this.close()
    }
  }
}
</script>

<style scoped>
.modal-form {
  max-width: 100%;
}

.password-strength {
  margin-top: 5px;
  font-size: 12px;
  color: #757575;
}

.department-hint {
  margin-top: 5px;
  font-size: 12px;
  color: #1976d2;
}
</style>

