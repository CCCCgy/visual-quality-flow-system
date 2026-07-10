package com.example.visualqms.vo;

import lombok.Data;

/**
 * 文件职责：
 * 返回 YOLO JSON 导入结果统计。
 *
 * 所属层级：
 * VO。
 *
 * 数据来源：
 * DetectionImportServiceImpl 在事务完成后组装 taskId、imageId、imageCount 和 detectionCount。
 *
 * 下游依赖：
 * 接口调用方可据此确认导入影响了哪张图片、写入了多少条检测结果。
 */
@Data
public class DetectionImportResultVO {

    private Long taskId;

    private Long imageId;

    private Integer imageCount;

    private Integer detectionCount;
}
