import { request } from '@/api/request/request'

export async function getAuditLogs({
  page = 1,
  page_size = 10,
  operator,
  operation_type,
  start_time,
  end_time
} = {}) {
  return request({
    method: 'GET',
    endpoint: '/audit-logs',
    query: { page, page_size, operator, operation_type, start_time, end_time }
  })
}

export async function getAuditLogDetail(id) {
  return request({
    method: 'GET',
    endpoint: `/audit-logs/${id}`
  })
}

