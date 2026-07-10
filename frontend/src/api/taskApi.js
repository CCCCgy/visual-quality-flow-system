import request from './request'

/**
 * 模块职责：
 * 检测任务相关前端 API，服务于 InspectionTaskListView.vue 和 BatchDetailView.vue。
 *
 * 对应后端 Controller：
 * InspectionTaskController。
 */

/**
 * 分页查询检测任务。
 *
 * 后端调用链：
 * GET /api/inspection-tasks
 * -> InspectionTaskController#pageTasks
 * -> InspectionTaskService
 * -> InspectionTaskMapper
 * -> inspection_task
 *
 * @param {object} params taskNo/batchId/status/pageNo/pageSize
 * @returns {Promise<object>} PageResult<InspectionTaskVO>
 */
export function getInspectionTaskPage(params) {
  return request({
    url: '/inspection-tasks',
    method: 'get',
    params
  })
}

/**
 * 获取检测任务详情。
 *
 * @param {number|string} id inspection_task 主键
 * @returns {Promise<object>} InspectionTaskVO
 */
export function getInspectionTaskDetail(id) {
  return request({
    url: `/inspection-tasks/${id}`,
    method: 'get'
  })
}

/**
 * 查询某个批次下的检测任务。
 *
 * 后端调用链：
 * GET /api/inspection-tasks/by-batch/{batchId}
 * -> InspectionTaskController#listTasksByBatch
 * -> InspectionTaskService
 * -> InspectionTaskMapper
 * -> inspection_task
 *
 * @param {number|string} batchId production_batch 主键
 * @returns {Promise<Array>} InspectionTaskVO[]
 */
export function getInspectionTasksByBatch(batchId) {
  return request({
    url: `/inspection-tasks/by-batch/${batchId}`,
    method: 'get'
  })
}
