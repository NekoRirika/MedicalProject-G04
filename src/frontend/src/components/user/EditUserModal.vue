<template>
  <Modal v-model:visible="innerVisible" title="编辑用户" width-preset="narrow">
    <form id="edit-user-form" class="edit-form" @submit.prevent="submit">
      <div class="form-group">
        <label for="edit-username">用户名</label>
        <input id="edit-username" type="text" :value="user?.username || ''" disabled class="input-disabled">
      </div>
      <div class="form-group">
        <label for="edit-name">姓名 *</label>
        <input id="edit-name" v-model="form.name" type="text" required placeholder="请输入姓名">
      </div>
      <div class="form-group">
        <label for="edit-department">部门 *</label>
        <select id="edit-department" v-model="form.department" required>
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
        </select>
      </div>
      <div class="form-group">
        <label for="edit-role">角色 *</label>
        <select id="edit-role" v-model="form.role" required>
          <option value="admin">管理员</option>
          <option value="doctor">医生</option>
          <option value="researcher">科研人员</option>
        </select>
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
  name: 'EditUserModal',
  components: { Modal },
  props: {
    visible: { type: Boolean, default: false },
    user: {
      type: Object,
      default: null
    }
  },
  emits: ['update:visible', 'saved'],
  data() {
    return {
      form: {
        name: '',
        department: '',
        role: 'doctor'
      }
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
      if (val && this.user) this.syncFromUser()
    },
    user: {
      deep: true,
      handler(u) {
        if (this.visible && u) this.syncFromUser()
      }
    }
  },
  methods: {
    normalizeRole(role) {
      const v = String(role || '').trim()
      if (v === 'admin' || v === 'doctor' || v === 'researcher') return v
      return 'doctor'
    },
    syncFromUser() {
      const u = this.user
      if (!u) return
      this.form = {
        name: u.name || '',
        department: u.department || '',
        role: this.normalizeRole(u.role)
      }
    },
    close() {
      this.innerVisible = false
    },
    submit() {
      const u = this.user
      if (!u || !u.id) {
        toast.warning('未选择用户')
        return
      }
      const { name, department, role } = this.form
      if (!name?.trim() || !department?.trim()) {
        toast.warning('请填写姓名与部门')
        return
      }
      this.$emit('saved', {
        id: u.id,
        payload: { name: name.trim(), department: department.trim(), role }
      })
    }
  }
}
</script>

<style scoped>
.edit-form {
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

.form-group input,
.form-group select,
.input-disabled {
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.input-disabled {
  background: #f5f5f5;
  color: #666;
}
</style>
