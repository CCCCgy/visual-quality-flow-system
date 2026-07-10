package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 返回给检测任务列表和批次详情页的任务视图对象。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * inspection_task 单表。
 *
 * 下游依赖：
 * InspectionTaskListView.vue 和 BatchDetailView.vue 根据 id 跳转到检测结果列表。
 */
@Data
public class InspectionTaskVO {

    private Long id;

    private String taskNo;

    private Long batchId;

    private String modelName;

    private String modelVersion;

    private String sourceType;

    private String status;

    private Long createdBy;

    private LocalDateTime importedTime;

    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
