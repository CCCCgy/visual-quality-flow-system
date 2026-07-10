import request from './request'

/**
 * 模块职责：
 * 检测结果相关前端 API，服务于 DetectionResultListView.vue 和 DetectionResultDetailView.vue。
 *
 * 对应后端 Controller：
 * DetectionController。
 */

/**
 * 分页查询检测结果。
 *
 * 后端调用链：
 * GET /api/detections
 * -> DetectionController#pageDetectionResults
 * -> DetectionResultService
 * -> DetectionResultMapper
 * -> detection_result
 *
 * @param {object} params taskId/imageId/className/status/pageNo/pageSize
 * @returns {Promise<object>} PageResult<DetectionResultVO>
 */
export function getDetectionResultPage(params) {
  return request({
    url: '/detections',
    method: 'get',
    params
  })
}

/**
 * 获取检测结果基础详情。
 *
 * @param {number|string} id detection_result 主键
 * @returns {Promise<object>} DetectionResultVO
 */
export function getDetectionResultDetail(id) {
  return request({
    url: `/detections/${id}`,
    method: 'get'
  })
}

/**
 * 获取某个检测结果的视觉详情。
 *
 * 调用方：
 * DetectionResultDetailView.vue。
 *
 * 后端调用链：
 * GET /api/detections/{id}/visual-detail
 * -> DetectionController#getDetectionVisualDetail
 * -> DetectionResultService
 * -> DetectionResultMapper / InspectionImageMapper
 * -> detection_result / inspection_image
 *
 * @param {number|string} id detection_result 主键
 * @returns {Promise<object>} 检测结果、图片信息和 bbox 坐标
 */
export function getDetectionVisualDetail(id) {
  return request({
    url: `/detections/${id}/visual-detail`,
    method: 'get'
  })
}
