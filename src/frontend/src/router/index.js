import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Index from '../views/Index.vue'
import UserManagement from '../views/UserManagement.vue'
import CaseManagement from '../views/CaseManagement.vue'
import CaseCreate from '../views/CaseCreate.vue'
import CaseDetail from '../views/CaseDetail.vue'
import DetectionResult from '../views/DetectionResult.vue'
import ModelManagement from '../views/ModelManagement.vue'
import Feedback from '../views/Feedback.vue'
import AuditLog from '../views/AuditLog.vue'
import Profile from '../views/Profile.vue'
import userStore from '../store/userStore'
import { getToken } from '@/api/request/tokenStore'

const routes = [
  { path: '/login', component: Login },
  { path: '/', component: Index },
  { path: '/user-management', component: UserManagement, meta: { requiresAdmin: true } },
  { path: '/case-management', component: CaseManagement, meta: { requiresDoctorOrResearcher: true } },
  { path: '/case-create', component: CaseCreate, meta: { requiresDoctor: true } },
  { path: '/case-detail/:id', component: CaseDetail, meta: { requiresDoctorOrResearcher: true } },
  { path: '/detection-result/:caseId/:detectionId', component: DetectionResult, meta: { requiresDoctorOrResearcher: true } },
  { path: '/model-management', component: ModelManagement, meta: { requiresAdminOrResearcher: true } },
  { path: '/feedback', component: Feedback, meta: { requiresDoctorOrResearcher: true } },
  { path: '/audit-log', component: AuditLog, meta: { requiresAdmin: true } },
  { path: '/profile', component: Profile }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 强制登录：除 /login 外，必须有 token 才能进入
  const token = getToken()
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }

  // 检查是否需要管理员权限
  if (to.meta.requiresAdmin) {
    if (userStore.isAdmin()) {
      next()
    } else {
      next('/login')
    }
  }
  // 检查是否需要医生权限
  else if (to.meta.requiresDoctor) {
    if (userStore.isDoctor()) {
      next()
    } else {
      next('/login')
    }
  }
  // 检查是否需要医生或科研人员权限
  else if (to.meta.requiresDoctorOrResearcher) {
    if (userStore.isDoctor() || userStore.isResearcher()) {
      next()
    } else {
      next('/login')
    }
  }
  // 检查是否需要管理员或科研人员权限
  else if (to.meta.requiresAdminOrResearcher) {
    if (userStore.isAdmin() || userStore.isResearcher()) {
      next()
    } else {
      next('/login')
    }
  }
  // 其他页面直接通过
  else {
    next()
  }
})

export default router