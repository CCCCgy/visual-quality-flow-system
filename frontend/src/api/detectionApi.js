import request from './request'

export function getDetectionResultPage(params) {
  return request({
    url: '/detections',
    method: 'get',
    params
  })
}

export function getDetectionResultDetail(id) {
  return request({
    url: `/detections/${id}`,
    method: 'get'
  })
}
