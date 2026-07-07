import request from './request'

export function createCapa(data) {
  return request({
    url: '/capas',
    method: 'post',
    data
  })
}

export function getCapaPage(params) {
  return request({
    url: '/capas',
    method: 'get',
    params
  })
}

export function getCapaDetail(id) {
  return request({
    url: `/capas/${id}`,
    method: 'get'
  })
}

export function getCapaByNcr(ncrId) {
  return request({
    url: `/capas/by-ncr/${ncrId}`,
    method: 'get'
  })
}

export function updateCapa(id, data) {
  return request({
    url: `/capas/${id}`,
    method: 'put',
    data
  })
}

export function updateCapaStatus(id, status) {
  return request({
    url: `/capas/${id}/status`,
    method: 'patch',
    data: { status }
  })
}
