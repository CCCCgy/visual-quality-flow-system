package com.example.visualqms.vo;

import lombok.Data;

/**
 * 文件职责：
 * 承载按 status 分组统计的单行结果。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * DashboardServiceImpl 对批次、检测结果、NCR、CAPA 表执行 status + count 分组。
 *
 * 下游依赖：
 * DashboardView.vue 将 status 映射为 ECharts 饼图或柱状图名称。
 */
@Data
public class StatusCountVO {

    private String status;

    private Long count;
}
