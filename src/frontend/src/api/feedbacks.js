import { request } from '@/api/request/request'

/**
 * 获取评价反馈列表（v1.1）GET /api/feedbacks
 * @param {{ page?: number, pageSize?: number }} query — 与 FeedbackController 一致使用 pageSize
 * @returns {Promise<{ total: number, data: Array, totalPages?: number, pageSize?: number, page?: number }>}
 * 列表项字段见文档 6.5：id, caseNo, patientName, detectTime, evaluation, feedbackTime, operator, feedback。
 * 若后端扩展返回病例主键 caseId、检测 detectionId，可用于跳转检测结果页。
 */
export async function getFeedbacks({ page = 1, pageSize = 10 } = {}) {
  return request({
    method: 'GET',
    endpoint: '/feedbacks',
    query: { page, pageSize }
  })
}
