package com.example.visualqms.service;

import com.example.visualqms.dto.DetectionImportDTO;
import com.example.visualqms.vo.DetectionImportResultVO;

/**
 * 文件职责：
 * 定义 YOLO JSON 导入业务能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * DetectionController#importYoloJson。
 *
 * 下游依赖：
 * 由 DetectionImportServiceImpl 实现，访问 inspection_task、inspection_image 和 detection_result。
 *
 * 设计说明：
 * 导入不是单表写入，而是多表状态推进；通过 Service 接口集中事务边界和转换规则。
 */
public interface DetectionImportService {

    /** 导入 YOLO JSON，对应 POST /api/detections/import-json。 */
    DetectionImportResultVO importYoloJson(DetectionImportDTO dto);
}
