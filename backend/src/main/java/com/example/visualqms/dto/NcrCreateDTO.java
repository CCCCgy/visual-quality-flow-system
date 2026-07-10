package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件职责：
 * 接收 POST /api/ncrs 的 NCR 创建请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * ReviewListView.vue 通过 ncrApi.js 在 CONFIRMED_DEFECT 记录上创建 NCR。
 *
 * 下游依赖：
 * NcrRecordServiceImpl 根据 reviewId 追溯 detection_result、inspection_task 和 production_batch。
 */
@Data
public class NcrCreateDTO {

    @NotBlank(message = "ncrNo cannot be blank")
    private String ncrNo;

    @NotNull(message = "reviewId cannot be null")
    private Long reviewId;

    @NotBlank(message = "severity cannot be blank")
    private String severity;

    @NotBlank(message = "description cannot be blank")
    private String description;

    @NotNull(message = "createdBy cannot be null")
    private Long createdBy;
}
