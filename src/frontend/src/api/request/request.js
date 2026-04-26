import { API_BASE_URL, API_PREFIX } from '@/api/config'
import { getToken, clearToken } from '@/api/request/tokenStore'
import userStore from '@/store/userStore'

function buildUrl(endpoint, query) {
  const url = new URL(`${API_BASE_URL}${API_PREFIX}${endpoint}`)
  if (query && typeof query === 'object') {
    Object.entries(query).forEach(([k, v]) => {
      if (v === undefined || v === null || v === '') return
      url.searchParams.set(k, String(v))
    })
  }
  return url.toString()
}

function isFormData(body) {
  return typeof FormData !== 'undefined' && body instanceof FormData
}

async function handleJsonResponse(res) {
  const json = await res.json().catch(() => null)
  if (!json) {
    throw new Error(`接口返回无法解析(JSON)：HTTP ${res.status}`)
  }
  if (json.code !== 200) {
    // 后端错误码：非 200 视为失败
    throw new Error(json.message || `请求失败，code=${json.code}`)
  }
  return json.data
}

/**
 * 通用 request（所有接口统一走这里）
 * @param {object} options
 * @param {'GET'|'POST'|'PUT'|'DELETE'} options.method
 * @param {string} options.endpoint - 形如 '/auth/login'（不需要带 /api）
 * @param {object} [options.query]
 * @param {object|FormData} [options.body]
 * @param {object} [options.headers]
 * @param {boolean} [options.auth] - 是否需要附带鉴权（默认 true）
 * @param {'json'|'blob'} [options.responseType]
 */
export async function request(options) {
  const {
    method,
    endpoint,
    query,
    body,
    headers = {},
    auth = true,
    responseType = 'json'
  } = options

  const token = auth ? getToken() : null
  const reqHeaders = { ...headers }

  if (auth && token) {
    reqHeaders.Authorization = `Bearer ${token}`
  }

  let reqBody = body
  if (body !== undefined && body !== null && !isFormData(body)) {
    // JSON 请求默认序列化
    reqHeaders['Content-Type'] = reqHeaders['Content-Type'] || 'application/json'
    reqBody = JSON.stringify(body)
  }

  const res = await fetch(buildUrl(endpoint, query), {
    method,
    headers: reqHeaders,
    body: reqBody
  })

  // 401：登录态失效才清理 token；403 多为“权限不足”，不应当清登录态
  if (res.status === 401) {
    clearToken()
    userStore.clearUser()
  }

  if (responseType === 'blob') {
    // 文件下载：不使用统一 code/message 包装解析（按文档示例导出一般是文件流）
    return res.blob()
  }

  return handleJsonResponse(res)
}

