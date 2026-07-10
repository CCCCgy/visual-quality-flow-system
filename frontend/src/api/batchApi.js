import request from './request'

/**
 * 模块职责：
 * 批次相关前端 API，服务于 BatchListView.vue 和 BatchDetailView.vue。
 *
 * 对应后端 Controller：
 * ProductionBatchController。
 *
 * 调用链：
 * 页面 -> batchApi.js -> request.js -> /api/batches -> ProductionBatchController。
 */

/**
 * 分页查询生产批次。
 *
 * 后端调用链：
 * GET /api/batches
 * -> ProductionBatchController#pageBatches
 * -> ProductionBatchService
 * -> ProductionBatchMapper
 * -> production_batch
 *
 * @param {object} params batchNo/status/pageNo/pageSize
 * @returns {Promise<object>} PageResult<ProductionBatchVO>
 */
export function getBatchPage(params) {
  return request({
    url: '/batches',
    method: 'get',
    params
  })
}

/**
 * 获取批次详情。
 *
 * @param {number|string} id production_batch 主键
 * @returns {Promise<object>} ProductionBatchVO
 */
export function getBatchDetail(id) {
  return request({
    url: `/batches/${id}`,
    method: 'get'
  })
}
