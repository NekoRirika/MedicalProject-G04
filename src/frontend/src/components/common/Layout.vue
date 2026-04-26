<template>
  <div class="app-container">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1>胸有成影</h1>
      </div>
      <nav class="sidebar-menu">
        <router-link to="/" class="menu-item" :class="{ active: currentPage === 'index' }">首页</router-link>

        <router-link
          v-if="userStore.isDoctor() || userStore.isResearcher()"
          to="/case-management"
          class="menu-item"
          :class="{ active: currentPage === 'case' }"
        >病例管理</router-link>

        <router-link
          v-if="userStore.isAdmin() || userStore.isResearcher()"
          to="/model-management"
          class="menu-item"
          :class="{ active: currentPage === 'model' }"
        >模型管理</router-link>

        <router-link
          v-if="userStore.isDoctor() || userStore.isResearcher()"
          to="/feedback"
          class="menu-item"
          :class="{ active: currentPage === 'feedback' }"
        >评价反馈</router-link>

        <router-link
          v-if="userStore.isAdmin()"
          to="/user-management"
          class="menu-item"
          :class="{ active: currentPage === 'user' }"
        >用户管理</router-link>

        <router-link
          v-if="userStore.isAdmin()"
          to="/audit-log"
          class="menu-item"
          :class="{ active: currentPage === 'audit' }"
        >审计日志</router-link>

        <router-link to="/profile" class="menu-item" :class="{ active: currentPage === 'profile' }">个人中心</router-link>

        <a
          href="#"
          class="menu-item menu-item--logout"
          @click.prevent="openLogoutConfirm"
        >退出登录</a>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-nav">
        <div class="nav-left">
          <h2>{{ title }}</h2>
        </div>
        <div class="nav-right">
          <div class="user-info">
            <div class="user-avatar" :title="headerRoleHint">{{ headerAvatar }}</div>
            <span class="user-display-name">{{ headerName }}</span>
          </div>
        </div>
      </header>

      <div class="content-area">
        <slot />
      </div>
    </main>

    <LogoutConfirmDialog
      v-model:visible="showLogoutConfirm"
      :loading="logoutLoading"
      @confirm="doLogout"
    />
  </div>
</template>

<script>
import LogoutConfirmDialog from '@/components/common/LogoutConfirmDialog.vue'
import { logout } from '@/api/auth'
import userStore from '@/store/userStore'
import { toast } from '@/utils/toast'

export default {
  name: 'Layout',
  components: { LogoutConfirmDialog },
  props: {
    title: { type: String, default: '首页' },
    currentPage: { type: String, default: 'index' },
    /** 无本地用户缓存时的兜底（一般登录后不再使用） */
    userName: { type: String, default: '未登录' },
    userAvatar: { type: String, default: '?' }
  },
  data() {
    return {
      userStore,
      showLogoutConfirm: false,
      logoutLoading: false
    }
  },
  computed: {
    headerAvatar() {
      if (userStore.user) {
        return userStore.roleAvatarLetter()
      }
      return this.userAvatar
    },
    headerName() {
      if (userStore.user) {
        const n = userStore.displayName()
        if (n) return n
      }
      return this.userName
    },
    headerRoleHint() {
      const r = userStore.getRole()
      if (r === 'admin') return '管理员'
      if (r === 'doctor') return '医生'
      if (r === 'researcher') return '科研人员'
      return ''
    }
  },
  methods: {
    openLogoutConfirm() {
      this.showLogoutConfirm = true
    },
    async doLogout() {
      this.logoutLoading = true
      try {
        await logout()
        toast.success('已退出登录')
      } catch (e) {
        toast.warning(e.message || '登出接口异常，已清除本地登录状态')
      } finally {
        this.logoutLoading = false
        this.showLogoutConfirm = false
        this.$router.replace('/login')
      }
    }
  }
}
</script>

<style scoped>
.user-display-name {
  font-weight: 500;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
