package com.example.visualqms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/**
 * 文件职责：
 * 接收 POST /api/capas 的 CAPA 创建请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * NcrListView.vue 通过 capaApi.js 在 OPEN NCR 上创建 CAPA。
 *
 * 下游依赖：
 * CapaRecordServiceImpl 写入 capa_record，并同步 ncr_record 与 production_batch 状态。
 */
@Data
public class CapaCreateDTO {

    @NotBlank(message = "capaNo cannot be blank")
    private String capaNo;

    @NotNull(message = "ncrId cannot be null")
    private Long ncrId;

    @NotNull(message = "ownerId cannot be null")
    private Long ownerId;

    private String rootCause;

    private String correctiveAction;

    private String preventiveAction;

    private LocalDate dueDate;
}
