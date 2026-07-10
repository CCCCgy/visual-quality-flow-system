import request from './request'

/**
 * 模块职责：
 * 人工复核相关前端 API，服务于检测结果列表、检测结果详情和复核列表。
 *
 * 对应后端 Controller：
 * ReviewRecordController。
 */

/**
 * 创建人工复核记录。
 *
 * 后端调用链：
 * POST /api/reviews
 * -> ReviewRecordController#createReview
 * -> ReviewRecordService
 * -> ReviewRecordMapper / DetectionResultMapper
 * -> review_record / detection_result
 *
 * @param {object} data detectionResultId/reviewerId/reviewResult/reviewComment
 * @returns {Promise<object>} ReviewRecordVO
 */
export function createReview(data) {
  return request({
    url: '/reviews',
    method: 'post',
    data
  })
}

/**
 * 分页查询复核记录。
 *
 * @param {object} params taskId/imageId/reviewerId/reviewResult/pageNo/pageSize
 * @returns {Promise<object>} PageResult<ReviewRecordVO>
 */
export function getReviewPage(params) {
  return request({
    url: '/reviews',
    method: 'get',
    params
  })
}

/**
 * 获取复核详情。
 *
 * @param {number|string} id review_record 主键
 * @returns {Promise<object>} ReviewRecordVO
 */
export function getReviewDetail(id) {
  return request({
    url: `/reviews/${id}`,
    method: 'get'
  })
}

/**
 * 根据检测结果查询复核记录。
 *
 * @param {number|string} detectionResultId detection_result 主键
 * @returns {Promise<object|null>} ReviewRecordVO 或 null
 */
export function getReviewByDetection(detectionResultId) {
  return request({
    url: `/reviews/by-detection/${detectionResultId}`,
    method: 'get'
  })
}
