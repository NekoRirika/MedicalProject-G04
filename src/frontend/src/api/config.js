// 统一配置：后端域名/端口与统一前缀
// - 开发环境：在 src/frontend/.env.development 中设置 VITE_API_BASE_URL（须含 /api）
// - 生产构建：在 .env.production 或 CI 环境变量中设置
// - 个人覆盖：.env.development.local（git 忽略 *.local，优先级高于 .env.development）
// 若未设置环境变量，默认连本机后端。
export const API_BASE_URL =
  (import.meta.env?.VITE_API_BASE_URL)?.replace(/\/$/, '') || 'http://localhost:8080/api'
export const API_PREFIX = ''

