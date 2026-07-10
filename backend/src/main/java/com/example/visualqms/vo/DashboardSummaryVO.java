package com.example.visualqms.vo;

import lombok.Data;

/**
 * 文件职责：
 * 返回 Dashboard 顶部汇总卡片数据。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * DashboardServiceImpl 聚合 production_batch、inspection_task、detection_result、ncr_record、capa_record。
 *
 * 下游依赖：
 * DashboardView.vue 的 summaryCards 计算属性将这些字段转换为页面卡片。
 */
@Data
public class DashboardSummaryVO {

    private Long batchCount;

    private Long taskCount;

    private Long detectionCount;

    private Long pendingReviewCount;

    private Long confirmedDefectCount;

    private Long openNcrCount;

    private Long inProgressCapaCount;

    private Long closedCapaCount;
}
