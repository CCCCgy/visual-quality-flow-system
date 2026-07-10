package com.example.visualqms.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 返回 CAPA 整改记录给列表、详情和按 NCR 查询接口。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * capa_record 单表。
 *
 * 下游依赖：
 * CapaListView.vue 根据 status 判断是否允许编辑、待验证、关闭或取消。
 */
@Data
public class CapaRecordVO {

    private Long id;

    private String capaNo;

    private Long ncrId;

    private Long batchId;

    private Long ownerId;

    private String rootCause;

    private String correctiveAction;

    private String preventiveAction;

    private String verifyResult;

    private String status;

    private LocalDate dueDate;

    private LocalDateTime closedTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
