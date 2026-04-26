// token 保存在 sessionStorage 中：
// - 刷新页面可复用
// - 关闭标签页后自动清除
const TOKEN_KEY = 'jwt_token'

export function getToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token) {
  try {
    if (!token) sessionStorage.removeItem(TOKEN_KEY)
    else sessionStorage.setItem(TOKEN_KEY, token)
  } catch {
    // no-op
  }
}

export function clearToken() {
  try {
    sessionStorage.removeItem(TOKEN_KEY)
  } catch {
    // no-op
  }
}

