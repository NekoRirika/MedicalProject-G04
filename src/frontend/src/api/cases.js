import { request } from '@/api/request/request'

export async function getCases({ page = 1, page_size = 10, case_id, patient_name } = {}) {
  return request({
    method: 'GET',
    endpoint: '/cases',
    query: { page, page_size, case_id, patient_name }
  })
}

export async function createCase(payload) {
  return request({
    method: 'POST',
    endpoint: '/cases',
    body: payload
  })
}

export async function getCaseDetail(id) {
  return request({
    method: 'GET',
    endpoint: `/cases/${id}`
  })
}

export async function updateCase(id, payload) {
  return request({
    method: 'PUT',
    endpoint: `/cases/${id}`,
    body: payload
  })
}

export async function deleteCase(id) {
  return request({
    method: 'DELETE',
    endpoint: `/cases/${id}`
  })
}

