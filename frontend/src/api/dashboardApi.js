import request from './request'

export function getDashboardSummary() {
  return request({
    url: '/dashboard/summary',
    method: 'get'
  })
}

export function getBatchStatusStats() {
  return request({
    url: '/dashboard/batch-status',
    method: 'get'
  })
}

export function getDetectionStatusStats() {
  return request({
    url: '/dashboard/detection-status',
    method: 'get'
  })
}

export function getDefectClassStats() {
  return request({
    url: '/dashboard/defect-class',
    method: 'get'
  })
}

export function getNcrStatusStats() {
  return request({
    url: '/dashboard/ncr-status',
    method: 'get'
  })
}

export function getCapaStatusStats() {
  return request({
    url: '/dashboard/capa-status',
    method: 'get'
  })
}
