import request from './request'

/**
 * 模块职责：
 * NCR 不合格记录相关前端 API，服务于 ReviewListView.vue 和 NcrListView.vue。
 *
 * 对应后端 Controller：
 * NcrRecordController。
 */

/**
 * 创建 NCR。
 *
 * 后端调用链：
 * POST /api/ncrs
 * -> NcrRecordController#createNcr
 * -> NcrRecordService
 * -> ReviewRecordMapper / DetectionResultMapper / InspectionTaskMapper / NcrRecordMapper / ProductionBatchMapper
 * -> review_record / detection_result / inspection_task / ncr_record / production_batch
 *
 * @param {object} data ncrNo/reviewId/severity/description/createdBy
 * @returns {Promise<object>} NcrRecordVO
 */
export function createNcr(data) {
  return request({
    url: '/ncrs',
    method: 'post',
    data
  })
}

/**
 * 分页查询 NCR。
 *
 * @param {object} params ncrNo/batchId/taskId/severity/status/pageNo/pageSize
 * @returns {Promise<object>} PageResult<NcrRecordVO>
 */
export function getNcrPage(params) {
  return request({
    url: '/ncrs',
    method: 'get',
    params
  })
}

/**
 * 获取 NCR 详情。
 *
 * @param {number|string} id ncr_record 主键
 * @returns {Promise<object>} NcrRecordVO
 */
export function getNcrDetail(id) {
  return request({
    url: `/ncrs/${id}`,
    method: 'get'
  })
}

/**
 * 根据复核记录查询 NCR。
 *
 * @param {number|string} reviewId review_record 主键
 * @returns {Promise<object|null>} NcrRecordVO 或 null
 */
export function getNcrByReview(reviewId) {
  return request({
    url: `/ncrs/by-review/${reviewId}`,
    method: 'get'
  })
}

/**
 * 更新 NCR 状态。
 *
 * @param {number|string} id ncr_record 主键
 * @param {string} status OPEN/CLOSED/CANCELLED
 * @returns {Promise<object>} NcrRecordVO
 */
export function updateNcrStatus(id, status) {
  return request({
    url: `/ncrs/${id}/status`,
    method: 'patch',
    data: { status }
  })
}
