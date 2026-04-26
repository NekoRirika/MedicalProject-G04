import { request } from '@/api/request/request'

export async function getDashboardStats() {
  return request({
    method: 'GET',
    endpoint: '/dashboard/stats'
  })
}
