import request from './request'

export function createNcr(data) {
  return request({
    url: '/ncrs',
    method: 'post',
    data
  })
}

export function getNcrPage(params) {
  return request({
    url: '/ncrs',
    method: 'get',
    params
  })
}

export function getNcrDetail(id) {
  return request({
    url: `/ncrs/${id}`,
    method: 'get'
  })
}

export function getNcrByReview(reviewId) {
  return request({
    url: `/ncrs/by-review/${reviewId}`,
    method: 'get'
  })
}

export function updateNcrStatus(id, status) {
  return request({
    url: `/ncrs/${id}/status`,
    method: 'patch',
    data: { status }
  })
}
