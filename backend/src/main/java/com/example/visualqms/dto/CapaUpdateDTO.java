package com.example.visualqms.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 文件职责：
 * 接收 PUT /api/capas/{id} 的 CAPA 内容编辑请求。
 *
 * 所属层级：
 * DTO。
 *
 * 上游调用：
 * CapaListView.vue 编辑弹窗通过 capaApi.js 提交。
 *
 * 下游依赖：
 * CapaRecordServiceImpl 仅更新非 null 字段，并禁止终态 CAPA 修改。
 */
@Data
public class CapaUpdateDTO {

    private String rootCause;

    private String correctiveAction;

    private String preventiveAction;

    private String verifyResult;

    private LocalDate dueDate;
}
