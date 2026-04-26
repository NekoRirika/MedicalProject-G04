import { request } from '@/api/request/request'

export async function startDetection(caseId, payload) {
  // payload: { modelId, parameters? }
  return request({
    method: 'POST',
    endpoint: `/cases/${caseId}/detection`,
    body: payload
  })
}

/**
 * 获取单次检测详情（v1.1+）
 * 响应除 imageResults（逐图含 detections/analysis）外，可能含根级 `detections`：
 * 扁平的检测框列表（与文档 6.2 一致时由后端返回）；前端对未知字段应容错。
 */
export async function getDetectionResult(caseId, detectionId) {
  return request({
    method: 'GET',
    endpoint: `/cases/${caseId}/detection/${detectionId}`
  })
}

export async function exportDetectionResult(caseId, detectionId, format = 'json') {
  return request({
    method: 'GET',
    endpoint: `/cases/${caseId}/detection/${detectionId}/export`,
    query: { format },
    responseType: 'blob'
  })
}

export async function submitDetectionFeedback(caseId, detectionId, payload) {
  // payload: { evaluation, feedback }
  return request({
    method: 'POST',
    endpoint: `/cases/${caseId}/detection/${detectionId}/feedback`,
    body: payload
  })
}

