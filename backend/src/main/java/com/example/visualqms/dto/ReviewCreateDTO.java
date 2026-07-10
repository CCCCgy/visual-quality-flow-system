package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件职责：
 * 接收 POST /api/reviews 的人工复核请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * DetectionResultListView.vue 或 DetectionResultDetailView.vue 通过 reviewApi.js 提交。
 *
 * 下游依赖：
 * ReviewRecordServiceImpl 写入 review_record，并同步 detection_result.status。
 */
@Data
public class ReviewCreateDTO {

    @NotNull(message = "detectionResultId cannot be null")
    private Long detectionResultId;

    @NotNull(message = "reviewerId cannot be null")
    private Long reviewerId;

    @NotBlank(message = "reviewResult cannot be blank")
    private String reviewResult;

    private String reviewComment;
}
