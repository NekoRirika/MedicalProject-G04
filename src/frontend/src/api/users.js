import { request } from '@/api/request/request'

export async function getUsers({ page = 1, page_size = 10, username, department } = {}) {
  return request({
    method: 'GET',
    endpoint: '/users',
    query: { page, page_size, username, department }
  })
}

export async function createUser(payload) {
  // payload: { username, password, name, role, department }
  return request({
    method: 'POST',
    endpoint: '/users',
    body: payload
  })
}

export async function updateUser(id, payload) {
  // payload: { name, department, role }
  return request({
    method: 'PUT',
    endpoint: `/users/${id}`,
    body: payload
  })
}

export async function deleteUser(id) {
  return request({
    method: 'DELETE',
    endpoint: `/users/${id}`
  })
}

export async function resetPassword(id) {
  return request({
    method: 'POST',
    endpoint: `/users/${id}/reset-password`
  })
}

export async function updateUserStatus(id, payload) {
  // payload: { status: 'locked' | ... }
  return request({
    method: 'PUT',
    endpoint: `/users/${id}/status`,
    body: payload
  })
}

