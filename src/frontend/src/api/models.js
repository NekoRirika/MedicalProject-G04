import { request } from '@/api/request/request'

export async function getModels() {
  return request({
    method: 'GET',
    endpoint: '/models'
  })
}

export async function activateModel(modelId) {
  return request({
    method: 'POST',
    endpoint: `/models/${modelId}/activate`
  })
}

