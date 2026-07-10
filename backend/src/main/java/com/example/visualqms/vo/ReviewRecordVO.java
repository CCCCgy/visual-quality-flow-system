package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 返回人工复核记录给 ReviewListView 和复核查询接口。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * review_record 单表。
 *
 * 下游依赖：
 * ReviewListView.vue 根据 reviewResult 判断 CONFIRMED_DEFECT 是否允许创建 NCR。
 */
@Data
public class ReviewRecordVO {

    private Long id;

    private Long detectionResultId;

    private Long taskId;

    private Long imageId;

    private Long reviewerId;

    private String reviewResult;

    private String reviewComment;

    private LocalDateTime reviewedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
