import { request } from '@/api/request/request'

export async function getProfile() {
  return request({
    method: 'GET',
    endpoint: '/profile'
  })
}

export async function changePassword(payload) {
  // payload: { oldPassword, newPassword, confirmPassword }
  return request({
    method: 'PUT',
    endpoint: '/profile/password',
    body: payload
  })
}

