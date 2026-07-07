import request from './request'

export function getInspectionTaskPage(params) {
  return request({
    url: '/inspection-tasks',
    method: 'get',
    params
  })
}

export function getInspectionTaskDetail(id) {
  return request({
    url: `/inspection-tasks/${id}`,
    method: 'get'
  })
}

export function getInspectionTasksByBatch(batchId) {
  return request({
    url: `/inspection-tasks/by-batch/${batchId}`,
    method: 'get'
  })
}
