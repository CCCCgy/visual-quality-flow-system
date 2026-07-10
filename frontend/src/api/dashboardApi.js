import request from './request'

/**
 * 模块职责：
 * Dashboard 看板统计 API，服务于 DashboardView.vue。
 *
 * 对应后端 Controller：
 * DashboardController。
 *
 * 调用链：
 * DashboardView.vue -> dashboardApi.js -> GET /api/dashboard/*
 * -> DashboardController -> DashboardService -> 对应 Mapper -> VO -> ECharts。
 */

/** 获取顶部汇总卡片数据，返回 DashboardSummaryVO。 */
export function getDashboardSummary() {
  return request({
    url: '/dashboard/summary',
    method: 'get'
  })
}

/** 获取批次状态分布，返回 StatusCountVO[]，来自 production_batch。 */
export function getBatchStatusStats() {
  return request({
    url: '/dashboard/batch-status',
    method: 'get'
  })
}

/** 获取检测结果状态分布，返回 StatusCountVO[]，来自 detection_result。 */
export function getDetectionStatusStats() {
  return request({
    url: '/dashboard/detection-status',
    method: 'get'
  })
}

/** 获取缺陷类别分布，返回 ClassCountVO[]，来自 detection_result.class_name。 */
export function getDefectClassStats() {
  return request({
    url: '/dashboard/defect-class',
    method: 'get'
  })
}

/** 获取 NCR 状态分布，返回 StatusCountVO[]，来自 ncr_record。 */
export function getNcrStatusStats() {
  return request({
    url: '/dashboard/ncr-status',
    method: 'get'
  })
}

/** 获取 CAPA 状态分布，返回 StatusCountVO[]，来自 capa_record。 */
export function getCapaStatusStats() {
  return request({
    url: '/dashboard/capa-status',
    method: 'get'
  })
}
