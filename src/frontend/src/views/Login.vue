<template>
  <div class="login-page">
    <div class="login-page-bg login-page-bg--one"></div>
    <div class="login-page-bg login-page-bg--two"></div>

    <div class="login-shell">
      <section class="login-hero">
        <div class="login-badge">AI Medical Imaging Platform</div>
        <h1 class="login-title">
          <span class="login-title__brand">胸有成影</span>
          <span class="login-title__sub">医疗影像数据管理系统</span>
        </h1>
        <p class="login-subtitle">
          面向病例管理、医学影像与智能检测流程的一体化协作平台。
        </p>

        <div class="login-metrics">
          <div class="login-metric-card">
            <span class="login-metric-label">病例管理</span>
            <strong class="login-metric-value">统一归档</strong>
          </div>
          <div class="login-metric-card">
            <span class="login-metric-label">影像分析</span>
            <strong class="login-metric-value">智能辅助</strong>
          </div>
          <div class="login-metric-card">
            <span class="login-metric-label">审计留痕</span>
            <strong class="login-metric-value">全程可追踪</strong>
          </div>
        </div>

        <ul class="login-feature-list">
          <li class="login-feature-item">
            <span class="login-feature-dot"></span>
            医生、科研与管理角色分工清晰，业务流转更高效。
          </li>
          <li class="login-feature-item">
            <span class="login-feature-dot"></span>
            覆盖病例、影像、检测结果与人工反馈的完整闭环。
          </li>
          <li class="login-feature-item">
            <span class="login-feature-dot"></span>
            提供结构化审计记录，便于过程追踪与质量核查。
          </li>
        </ul>
      </section>

      <section class="login-panel">
        <div class="login-panel-head">
          <span class="login-panel-kicker">欢迎回来</span>
          <h2 class="login-panel-title">登录系统</h2>
          <p class="login-panel-desc">请输入账号信息以继续访问工作台。</p>
        </div>

        <form class="login-form" @submit.prevent="handleLogin">
          <div class="login-field">
            <label class="login-label" for="username">用户名</label>
            <input
              id="username"
              v-model="username"
              class="login-input"
              name="username"
              type="text"
              autocomplete="username"
              placeholder="请输入用户名"
              required
            >
          </div>

          <div class="login-field">
            <label class="login-label" for="password">密码</label>
            <div class="password-input-wrap">
              <input
                id="password"
                :type="showPassword ? 'text' : 'password'"
                v-model="password"
                class="login-input login-input--password"
                name="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                required
              >
              <button
                type="button"
                class="pwd-toggle-btn"
                @click.stop.prevent="showPassword = !showPassword"
              >
                {{ showPassword ? '隐藏' : '显示' }}
              </button>
            </div>
          </div>

          <button
            type="submit"
            class="login-submit"
            :disabled="submitting"
          >
            <span v-if="submitting" class="login-submit-spinner"></span>
            {{ submitting ? '登录中…' : '进入系统' }}
          </button>
        </form>

        <p class="login-panel-footer">
          登录后系统将按角色自动跳转到对应工作页面。
        </p>
      </section>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/auth'
import { toast } from '@/utils/toast'

export default {
  name: 'Login',
  data() {
    return {
      username: '',
      password: '',
      showPassword: false,
      submitting: false
    }
  },
  methods: {
    async handleLogin() {
      if (!this.username || !this.password) {
        toast.warning('请输入用户名和密码')
        return
      }

      try {
        this.submitting = true
        const data = await login({ username: this.username, password: this.password })
        const role = data?.user?.role
        if (role === 'admin') {
          this.$router.push('/user-management')
          return
        }
        if (role === 'researcher') {
          this.$router.push('/model-management')
          return
        }
        // doctor 或其它兜底
        this.$router.push('/')
      } catch (err) {
        toast.error(err.message || '登录失败')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background:
    radial-gradient(circle at top left, rgba(58, 123, 213, 0.18), transparent 38%),
    radial-gradient(circle at bottom right, rgba(0, 210, 255, 0.12), transparent 32%),
    linear-gradient(135deg, #eef5ff 0%, #f7fbff 44%, #f3f7fc 100%);
}

.login-page-bg {
  position: absolute;
  border-radius: 999px;
  filter: blur(10px);
  opacity: 0.55;
}

.login-page-bg--one {
  top: -80px;
  left: -90px;
  width: 260px;
  height: 260px;
  background: rgba(30, 136, 229, 0.2);
}

.login-page-bg--two {
  right: -60px;
  bottom: -60px;
  width: 220px;
  height: 220px;
  background: rgba(125, 211, 252, 0.22);
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(320px, 1.15fr) minmax(360px, 0.95fr);
  gap: 28px;
  width: min(1160px, 100%);
  align-items: stretch;
}

.login-hero,
.login-panel {
  border-radius: 26px;
  box-shadow: 0 24px 60px rgba(34, 72, 124, 0.12);
}

.login-hero {
  padding: 42px;
  background: linear-gradient(160deg, rgba(19, 78, 168, 0.95), rgba(30, 136, 229, 0.9));
  color: #fff;
}

.login-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.login-title {
  margin: 22px 0 14px;
  font-size: clamp(32px, 4vw, 44px);
  line-height: 1.15;
}

.login-title__brand,
.login-title__sub {
  display: block;
}

.login-title__brand {
  margin-bottom: 4px;
  letter-spacing: 0.04em;
}

.login-title__sub {
  font-size: clamp(18px, 2.1vw, 24px);
  font-weight: 600;
  color: rgba(255, 255, 255, 0.84);
}

.login-subtitle {
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.84);
  max-width: 560px;
}

.login-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.login-metric-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(4px);
}

.login-metric-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 8px;
}

.login-metric-value {
  font-size: 18px;
  font-weight: 700;
}

.login-feature-list {
  list-style: none;
  padding: 0;
  margin: 30px 0 0;
  display: grid;
  gap: 14px;
}

.login-feature-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.6;
}

.login-feature-dot {
  flex: 0 0 9px;
  height: 9px;
  margin-top: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 6px rgba(255, 255, 255, 0.08);
}

.login-panel {
  padding: 34px 34px 28px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(14px);
}

.login-panel-head {
  margin-bottom: 26px;
}

.login-panel-kicker {
  display: inline-block;
  color: #1e88e5;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.login-panel-title {
  margin: 8px 0 10px;
  font-size: 30px;
  color: #1c2733;
}

.login-panel-desc {
  margin: 0;
  color: #6b7886;
  line-height: 1.6;
}

.login-form {
  display: grid;
  gap: 18px;
}

.login-field {
  display: grid;
  gap: 8px;
}

.login-label {
  font-size: 14px;
  font-weight: 600;
  color: #2d3b48;
}

.login-input {
  width: 100%;
  box-sizing: border-box;
  min-height: 50px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #d9e2ec;
  background: rgba(255, 255, 255, 0.92);
  color: #1f2933;
  font-size: 15px;
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.login-input::placeholder {
  color: #9aa8b6;
}

.login-input:focus {
  border-color: #4da3ff;
  box-shadow: 0 0 0 4px rgba(30, 136, 229, 0.14);
  background: #fff;
}

.password-input-wrap {
  position: relative;
  display: flex;
  align-items: stretch;
}

.login-input--password {
  padding-right: 76px;
}

.pwd-toggle-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: rgba(30, 136, 229, 0.08);
  color: var(--primary-color, #1e88e5);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 7px 12px;
  border-radius: 999px;
  line-height: 1.2;
}

.pwd-toggle-btn:hover {
  background: rgba(30, 136, 229, 0.14);
}

.login-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  min-height: 52px;
  margin-top: 8px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 16px 28px rgba(30, 136, 229, 0.24);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.login-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(30, 136, 229, 0.28);
}

.login-submit:disabled {
  opacity: 0.76;
  cursor: not-allowed;
}

.login-submit-spinner {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: #fff;
  animation: loginSpin 0.8s linear infinite;
}

.login-panel-footer {
  margin: 18px 0 0;
  color: #71808f;
  font-size: 13px;
  line-height: 1.6;
}

@keyframes loginSpin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 640px;
  }

  .login-hero {
    padding: 30px 26px;
  }

  .login-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 18px 14px;
  }

  .login-panel,
  .login-hero {
    border-radius: 20px;
  }

  .login-panel {
    padding: 24px 20px;
  }

  .login-title {
    font-size: 28px;
  }

  .login-panel-title {
    font-size: 24px;
  }

}
</style>
