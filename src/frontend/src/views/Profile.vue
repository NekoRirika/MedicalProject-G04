<template>
  <Layout title="个人中心" current-page="profile">
    <div class="card">
      <div class="card-header">
        <h2>个人信息</h2>
      </div>
      <div class="form-container">
        <div class="form-row">
          <div class="form-group">
            <label>用户名</label>
            <input type="text" :value="profile?.username || ''" disabled>
          </div>
          <div class="form-group">
            <label>姓名</label>
            <input type="text" :value="profile?.name || ''" disabled>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>部门</label>
            <input type="text" :value="profile?.department || ''" disabled>
          </div>
          <div class="form-group">
            <label>角色</label>
            <input type="text" :value="roleLabel(profile?.role) || ''" disabled>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>创建时间</label>
            <input type="text" :value="formatDate(profile?.createdTime)" disabled>
          </div>
          <div class="form-group">
            <label>上次登录</label>
            <input type="text" :value="formatDateTime(profile?.lastLogin)" disabled>
          </div>
        </div>
      </div>
    </div>

    <div class="card card-account">
      <div class="card-header">
        <h2>账户安全</h2>
      </div>
      <div class="account-actions">
        <p class="hint">定期修改密码有助于保护账户安全。</p>
        <button type="button" class="btn btn-primary" @click="showPasswordModal = true">修改密码</button>
      </div>
    </div>

    <ChangePasswordModal v-model:visible="showPasswordModal" @success="onPasswordChange" />
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import ChangePasswordModal from '@/components/user/ChangePasswordModal.vue'
import { getProfile, changePassword as changePasswordApi } from '@/api/profile'
import { toast } from '@/utils/toast'
import userStore from '@/store/userStore'

export default {
  name: 'Profile',
  components: { Layout, ChangePasswordModal },
  data() {
    return {
      profile: null,
      showPasswordModal: false
    }
  },
  mounted() {
    this.loadProfile()
  },
  methods: {
    roleLabel(role) {
      if (role === 'admin') return '管理员'
      if (role === 'doctor') return '医生'
      if (role === 'researcher') return '科研人员'
      return role || ''
    },
    formatDate(date) {
      if (!date) return ''
      return String(date).slice(0, 10)
    },
    formatDateTime(date) {
      if (!date) return ''
      const s = String(date)
      if (s.includes('T')) return s.replace('T', ' ').slice(0, 16)
      return s
    },
    async loadProfile() {
      try {
        this.profile = await getProfile()
        if (this.profile) {
          userStore.patchUser(this.profile)
        }
      } catch (err) {
        toast.error(err.message || '获取个人信息失败')
      }
    },
    async onPasswordChange(form) {
      try {
        await changePasswordApi(form)
        toast.success('密码修改成功')
        this.showPasswordModal = false
      } catch (err) {
        toast.error(err.message || '修改密码失败')
      }
    }
  }
}
</script>

<style scoped>
.account-actions {
  padding: 8px 0 4px;
}

.hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: #616161;
}

.card-account .card-header h2 {
  margin: 0;
}
</style>
