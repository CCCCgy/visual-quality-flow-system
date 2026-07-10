package com.example.visualqms.vo;

import lombok.Data;

/**
 * 文件职责：
 * 承载按缺陷类别 class_name 分组统计的单行结果。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * DashboardServiceImpl 对 detection_result.class_name 执行 group by。
 *
 * 下游依赖：
 * DashboardView.vue 将 className/count 转换为缺陷类别柱状图。
 */
@Data
public class ClassCountVO {

    private String className;

    private Long count;
}
