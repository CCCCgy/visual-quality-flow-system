package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 返回 NCR 不合格记录给列表、详情和按复核查询接口。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * ncr_record 单表，保留 batchId、taskId、detectionResultId、reviewId 追溯链。
 *
 * 下游依赖：
 * NcrListView.vue 根据 status=OPEN 判断是否允许创建 CAPA、关闭或取消。
 */
@Data
public class NcrRecordVO {

    private Long id;

    private String ncrNo;

    private Long batchId;

    private Long taskId;

    private Long detectionResultId;

    private Long reviewId;

    private String severity;

    private String status;

    private String description;

    private Long createdBy;

    private LocalDateTime closedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
