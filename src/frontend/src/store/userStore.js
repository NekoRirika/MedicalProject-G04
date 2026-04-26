import { reactive } from 'vue'

const USER_KEY = 'session_user_profile'

function loadUserFromSession() {
  try {
    const raw = sessionStorage.getItem(USER_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

function saveUserToSession(user) {
  try {
    if (!user) {
      sessionStorage.removeItem(USER_KEY)
      return
    }
    const minimal = {
      id: user.id,
      username: user.username,
      name: user.name,
      role: user.role,
      department: user.department,
      status: user.status,
      createdAt: user.createdAt,
      lastLogin: user.lastLogin
    }
    sessionStorage.setItem(USER_KEY, JSON.stringify(minimal))
  } catch {
    // no-op
  }
}

// 用户信息保存在 sessionStorage 中：
// - 刷新页面可复用
// - 关闭标签页后自动清除
const userStore = reactive({
  user: loadUserFromSession(),

  setUser(user) {
    this.user = user
    saveUserToSession(user)
  },

  patchUser(partial) {
    if (!partial || typeof partial !== 'object') return
    if (!this.user) this.user = { ...partial }
    else this.user = { ...this.user, ...partial }
    saveUserToSession(this.user)
  },

  clearUser() {
    this.user = null
    saveUserToSession(null)
  },

  getRole() {
    return this.user?.role || null
  },

  hasRole(role) {
    return this.getRole() === role
  },

  isAdmin() {
    return this.hasRole('admin')
  },

  isDoctor() {
    return this.hasRole('doctor')
  },

  isResearcher() {
    return this.hasRole('researcher')
  },

  /** 顶栏头像字母：管理员 A、医生 D、研究员 R */
  roleAvatarLetter() {
    const r = this.getRole()
    if (r === 'admin') return 'A'
    if (r === 'doctor') return 'D'
    if (r === 'researcher') return 'R'
    return '?'
  },

  /** 顶栏显示姓名，优先 name */
  displayName() {
    const u = this.user
    if (!u) return ''
    const n = u.name
    if (n && String(n).trim()) return String(n).trim()
    if (u.username) return u.username
    return ''
  }
})

export default userStore
