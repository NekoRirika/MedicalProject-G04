import { request } from '@/api/request/request'
import { setToken, clearToken } from '@/api/request/tokenStore'
import userStore from '@/store/userStore'

export async function login(payload) {
  // payload: { username, password }
  const data = await request({
    method: 'POST',
    endpoint: '/auth/login',
    auth: false,
    body: payload
  })

  // data: { token, user: {...} }
  if (data && data.token) {
    setToken(data.token)
    userStore.setUser(data.user)
  }
  return data
}

export async function logout() {
  // 无论接口是否成功，均在 finally 中清除本地 token 与用户缓存，避免“假退出”
  try {
    return await request({
      method: 'POST',
      endpoint: '/auth/logout',
      auth: true,
      responseType: 'json'
    })
  } finally {
    clearToken()
    userStore.clearUser()
  }
}

