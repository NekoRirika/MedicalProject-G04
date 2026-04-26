import { request } from '@/api/request/request'

export async function getCaseImages(caseId) {
  return request({
    method: 'GET',
    endpoint: `/cases/${caseId}/images`
  })
}

export async function uploadCaseImage(caseId, file) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    method: 'POST',
    endpoint: `/cases/${caseId}/images`,
    body: formData
  })
}

export async function batchUploadCaseImages(caseId, files) {
  const formData = new FormData()
  for (const file of files) {
    formData.append('files', file)
  }

  return request({
    method: 'POST',
    endpoint: `/cases/${caseId}/images/batch`,
    body: formData
  })
}

export async function deleteCaseImage(caseId, imageId) {
  return request({
    method: 'DELETE',
    endpoint: `/cases/${caseId}/images/${imageId}`
  })
}

