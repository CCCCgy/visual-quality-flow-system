import request from './request'

export function createReview(data) {
  return request({
    url: '/reviews',
    method: 'post',
    data
  })
}

export function getReviewPage(params) {
  return request({
    url: '/reviews',
    method: 'get',
    params
  })
}

export function getReviewDetail(id) {
  return request({
    url: `/reviews/${id}`,
    method: 'get'
  })
}

export function getReviewByDetection(detectionResultId) {
  return request({
    url: `/reviews/by-detection/${detectionResultId}`,
    method: 'get'
  })
}
