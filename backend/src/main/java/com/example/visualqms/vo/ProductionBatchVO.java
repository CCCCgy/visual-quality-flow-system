package com.example.visualqms.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件职责：
 * 返回给批次列表和批次详情页面的批次视图对象。
 *
 * 所属层级：
 * VO。
 *
 * 上游调用：
 * ProductionBatchServiceImpl 从 ProductionBatch 转换得到。
 *
 * 下游依赖：
 * BatchListView.vue 和 BatchDetailView.vue 读取这些字段渲染表格、详情和状态标签。
 *
 * 设计说明：
 * VO 是接口输出契约，避免前端直接依赖 Entity。
 */
@Data
public class ProductionBatchVO {

    private Long id;

    private String batchNo;

    private String productCode;

    private String productName;

    private Integer plannedQuantity;

    private String status;

    private Long createdBy;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
