import request from './request'

/**
 * 模块职责：
 * CAPA 整改记录相关前端 API，服务于 NcrListView.vue 和 CapaListView.vue。
 *
 * 对应后端 Controller：
 * CapaRecordController。
 */

/**
 * 创建 CAPA。
 *
 * 后端调用链：
 * POST /api/capas
 * -> CapaRecordController#createCapa
 * -> CapaRecordService
 * -> NcrRecordMapper / CapaRecordMapper / ProductionBatchMapper
 * -> ncr_record / capa_record / production_batch
 *
 * @param {object} data capaNo/ncrId/ownerId/rootCause/correctiveAction/preventiveAction/dueDate
 * @returns {Promise<object>} CapaRecordVO
 */
export function createCapa(data) {
  return request({
    url: '/capas',
    method: 'post',
    data
  })
}

/**
 * 分页查询 CAPA。
 *
 * @param {object} params capaNo/ncrId/batchId/ownerId/status/pageNo/pageSize
 * @returns {Promise<object>} PageResult<CapaRecordVO>
 */
export function getCapaPage(params) {
  return request({
    url: '/capas',
    method: 'get',
    params
  })
}

/**
 * 获取 CAPA 详情。
 *
 * @param {number|string} id capa_record 主键
 * @returns {Promise<object>} CapaRecordVO
 */
export function getCapaDetail(id) {
  return request({
    url: `/capas/${id}`,
    method: 'get'
  })
}

/**
 * 根据 NCR 查询 CAPA。
 *
 * @param {number|string} ncrId ncr_record 主键
 * @returns {Promise<object|null>} CapaRecordVO 或 null
 */
export function getCapaByNcr(ncrId) {
  return request({
    url: `/capas/by-ncr/${ncrId}`,
    method: 'get'
  })
}

/**
 * 更新 CAPA 内容。
 *
 * @param {number|string} id capa_record 主键
 * @param {object} data 根因、措施、验证结果和计划日期
 * @returns {Promise<object>} CapaRecordVO
 */
export function updateCapa(id, data) {
  return request({
    url: `/capas/${id}`,
    method: 'put',
    data
  })
}

/**
 * 更新 CAPA 状态。
 *
 * 后端调用链：
 * PATCH /api/capas/{id}/status
 * -> CapaRecordController#updateCapaStatus
 * -> CapaRecordService
 * -> CapaRecordMapper / NcrRecordMapper / ProductionBatchMapper
 * -> capa_record / ncr_record / production_batch
 *
 * @param {number|string} id capa_record 主键
 * @param {string} status PENDING_VERIFY/CLOSED/CANCELLED 等
 * @returns {Promise<object>} CapaRecordVO
 */
export function updateCapaStatus(id, status) {
  return request({
    url: `/capas/${id}/status`,
    method: 'patch',
    data: { status }
  })
}
