<template>
  <div
    v-if="visible"
    class="logout-overlay"
    role="presentation"
    @click.self="onCancel"
  >
    <div
      class="logout-card"
      role="dialog"
      aria-modal="true"
      aria-labelledby="logout-dialog-title"
    >
      <div class="logout-card-head">
        <h3 id="logout-dialog-title" class="logout-card-title">退出登录</h3>
        <button type="button" class="logout-close" aria-label="关闭" @click="onCancel">×</button>
      </div>
      <p class="logout-card-msg">确定退出当前账号？退出后需重新登录。</p>
      <div class="logout-card-actions">
        <button type="button" class="btn btn-secondary" :disabled="loading" @click="onCancel">取消</button>
        <button type="button" class="btn btn-danger" :disabled="loading" @click="$emit('confirm')">
          {{ loading ? '退出中…' : '确定退出' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LogoutConfirmDialog',
  props: {
    visible: { type: Boolean, default: false },
    loading: { type: Boolean, default: false }
  },
  emits: ['update:visible', 'confirm'],
  methods: {
    onCancel() {
      if (this.loading) return
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style scoped>
.logout-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.45);
}

.logout-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  padding: 14px 16px 16px;
  box-sizing: border-box;
}

.logout-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.logout-card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color, #333);
}

.logout-close {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  color: #888;
  cursor: pointer;
  border-radius: 4px;
}

.logout-close:hover {
  background: #f0f0f0;
  color: #333;
}

.logout-card-msg {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 1.45;
  color: #555;
}

.logout-card-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
