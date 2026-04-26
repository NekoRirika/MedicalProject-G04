<template>
  <Layout title="用户管理" current-page="user">
    <div class="card">
      <div class="search-container">
        <input type="text" class="search-input" placeholder="请输入用户名" v-model="usernameQuery">
        <select class="search-input" v-model="departmentQuery" style="margin-left: 10px;">
          <option value="">全部部门</option>
          <option value="放射科">放射科</option>
          <option value="科研部">科研部</option>
          <option value="管理部">管理部</option>
          <option value="儿科">儿科</option>
          <option value="呼吸科">呼吸科</option>
          <option value="心内科">心内科</option>
          <option value="消化科">消化科</option>
          <option value="神经科">神经科</option>
          <option value="内分泌科">内分泌科</option>
          <option value="肾内科">肾内科</option>
          <option value="血液科">血液科</option>
          <option value="肿瘤科">肿瘤科</option>
          <option value="感染科">感染科</option>
          <option value="风湿免疫科">风湿免疫科</option>
          <option value="急诊科">急诊科</option>
          <option value="重症医学科">重症医学科</option>
        </select>
        <button class="search-btn" @click="search">搜索</button>
        <button class="btn btn-primary" style="margin-left: 10px;" @click="openCreateUserModal">创建账号</button>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>用户列表</h2>
      </div>
      <div class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>用户名</th>
              <th>姓名</th>
              <th>部门</th>
              <th>角色</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id ?? user.username">
              <td>{{ user.username }}</td>
              <td>{{ user.name }}</td>
              <td>{{ user.department }}</td>
              <td>{{ roleLabel(user.role) }}</td>
              <td>{{ statusLabel(user.status) }}</td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td>
                <button
                  class="btn btn-primary"
                  :disabled="isCurrentUser(user)"
                  :title="isCurrentUser(user) ? '不能编辑当前登录账号' : ''"
                  @click="editUser(user)"
                >编辑</button>
                <button
                  class="btn btn-secondary"
                  :disabled="isCurrentUser(user)"
                  :title="isCurrentUser(user) ? '不能重置当前登录账号密码' : ''"
                  @click="resetPassword(user)"
                >重置密码</button>
                <button
                  v-if="user.status === 'active'"
                  class="btn btn-danger"
                  :disabled="isCurrentUser(user)"
                  :title="isCurrentUser(user) ? '不能锁定当前登录账号' : ''"
                  @click="toggleUserStatus(user)"
                >锁定</button>
                <button
                  v-else
                  class="btn btn-success"
                  :disabled="isCurrentUser(user)"
                  :title="isCurrentUser(user) ? '不能解锁当前登录账号' : ''"
                  @click="toggleUserStatus(user)"
                >解锁</button>
                <button
                  type="button"
                  class="btn btn-danger"
                  style="margin-left: 4px;"
                  :disabled="isCurrentUser(user)"
                  :title="isCurrentUser(user) ? '不能删除当前登录账号' : ''"
                  @click="confirmDeleteUser(user)"
                >删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="total > 0" class="pagination">
        <span class="pagination-meta">共 {{ total }} 条，每页 {{ pageSize }} 条</span>
        <div class="pagination-btns">
          <button type="button" class="pg-btn" :disabled="page <= 1" @click="goPage(1)">首页</button>
          <button type="button" class="pg-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
          <span class="pagination-current">第 {{ page }} / {{ totalPages }} 页</span>
          <button type="button" class="pg-btn" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
          <button type="button" class="pg-btn" :disabled="page >= totalPages" @click="goPage(totalPages)">末页</button>
        </div>
      </div>
    </div>

    <CreateUserModal v-model:visible="showCreateUserModal" @created="onUserCreated" />
    <EditUserModal
      v-model:visible="showEditUserModal"
      :user="editingUser"
      @saved="onEditUserSaved"
    />
  </Layout>
</template>

<script>
import Layout from '@/components/common/Layout.vue'
import CreateUserModal from '@/components/user/CreateUserModal.vue'
import EditUserModal from '@/components/user/EditUserModal.vue'
import {
  getUsers,
  createUser,
  updateUser,
  deleteUser,
  resetPassword as resetPasswordApi,
  updateUserStatus as updateUserStatusApi
} from '@/api/users'
import { toast } from '@/utils/toast'
import userStore from '@/store/userStore'

export default {
  name: 'UserManagement',
  components: { Layout, CreateUserModal, EditUserModal },
  data() {
    return {
      usernameQuery: '',
      departmentQuery: '',
      showCreateUserModal: false,
      showEditUserModal: false,
      editingUser: null,
      page: 1,
      pageSize: 10,
      total: 0,
      users: []
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  watch: {
    showEditUserModal(val) {
      if (!val) this.editingUser = null
    }
  },
  mounted() {
    this.fetchUsers()
  },
  methods: {
    openCreateUserModal() {
      this.showCreateUserModal = true
    },
    onUserCreated(payload) {
      createUser(payload)
        .then(() => {
          toast.success('创建成功')
          this.page = 1
          return this.fetchUsers()
        })
        .catch((err) => {
          const errorMsg = err?.response?.data?.message || err.message || '创建失败'
          toast.error(errorMsg)
        })
    },
    async fetchUsers() {
      try {
        const data = await getUsers({
          page: this.page,
          page_size: this.pageSize,
          username: this.usernameQuery.trim() || undefined,
          department: this.departmentQuery.trim() || undefined
        })
        const list = data?.list ?? data?.items ?? []
        this.total = Number(data?.total ?? 0)
        this.users = list.map((item) => ({
          id: item.id,
          username: item.username,
          name: item.name,
          department: item.department,
          role: item.role,
          status: item.status,
          createdAt: item.createdTime
        }))
        if (this.page > this.totalPages && this.totalPages >= 1) {
          this.page = this.totalPages
          return this.fetchUsers()
        }
      } catch (error) {
        console.error('Error fetching users:', error)
        toast.error('获取用户列表失败：' + (error.message || '未知错误'))
      }
    },
    search() {
      this.page = 1
      return this.fetchUsers()
    },
    goPage(p) {
      const next = Number(p)
      if (!Number.isFinite(next) || next < 1 || next > this.totalPages) return
      if (next === this.page) return
      this.page = next
      return this.fetchUsers()
    },
    roleLabel(role) {
      if (role === 'admin') return '管理员'
      if (role === 'doctor') return '医生'
      if (role === 'researcher') return '科研人员'
      return role || '-'
    },
    statusLabel(status) {
      if (status === 'active') return '正常'
      if (status === 'locked') return '锁定'
      return status || '-'
    },
    formatDate(date) {
      if (!date) return '-'
      return String(date).slice(0, 10)
    },
    editUser(user) {
      if (this.isCurrentUser(user)) {
        toast.warning('不能编辑当前登录账号')
        return
      }
      this.editingUser = { ...user }
      this.showEditUserModal = true
    },
    async onEditUserSaved({ id, payload }) {
      try {
        await updateUser(id, payload)
        this.showEditUserModal = false
        this.editingUser = null
        await this.fetchUsers()
      } catch (err) {
        toast.error(err.message || '更新失败')
      }
    },
    async resetPassword(user) {
      if (this.isCurrentUser(user)) {
        toast.warning('不能重置当前登录账号密码')
        return
      }
      const data = await resetPasswordApi(user.id)
      toast.success(`密码重置成功，默认密码：${data}`)
    },
    async toggleUserStatus(user) {
      if (this.isCurrentUser(user)) {
        toast.warning(`不能${user.status === 'active' ? '锁定' : '解锁'}当前登录账号`)
        return
      }
      const nextStatus = user.status === 'active' ? 'locked' : 'active'
      await updateUserStatusApi(user.id, { status: nextStatus })
      await this.fetchUsers()
    },
    isCurrentUser(user) {
      const self = userStore.user
      if (!self || user?.id == null) return false
      return Number(self.id) === Number(user.id)
    },
    async confirmDeleteUser(user) {
      if (!user?.id) {
        toast.warning('无法删除：缺少用户 ID')
        return
      }
      if (this.isCurrentUser(user)) {
        toast.warning('不能删除当前登录账号')
        return
      }
      const ok = window.confirm(
        `确定要删除用户「${user.username}」吗？删除后不可恢复。`
      )
      if (!ok) return
      try {
        await deleteUser(user.id)
        toast.success('删除成功')
        await this.fetchUsers()
      } catch (err) {
        toast.error(err.message || '删除失败')
      }
    }
  }
}
</script>

<style scoped>
.pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0 4px;
  border-top: 1px solid #eee;
  margin-top: 8px;
}

.pagination-meta {
  font-size: 13px;
  color: #616161;
}

.pagination-btns {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pg-btn {
  padding: 6px 12px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.pg-btn:hover:not(:disabled) {
  background: #f5f5f5;
}

.pg-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pagination-current {
  font-size: 13px;
  color: #333;
  padding: 0 6px;
}
</style>
