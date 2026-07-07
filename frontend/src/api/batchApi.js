import request from './request'

export function getBatchPage(params) {
  return request({
    url: '/batches',
    method: 'get',
    params
  })
}

export function getBatchDetail(id) {
  return request({
    url: `/batches/${id}`,
    method: 'get'
  })
}
