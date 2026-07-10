package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.entity.DetectionResult;
import com.example.visualqms.vo.DetectionResultVO;
import com.example.visualqms.vo.DetectionVisualDetailVO;

/**
 * 文件职责：
 * 定义检测结果查询和视觉详情查询能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * DetectionController。
 *
 * 下游依赖：
 * 由 DetectionResultServiceImpl 实现，访问 detection_result 和 inspection_image。
 */
public interface DetectionResultService extends IService<DetectionResult> {

    /** 分页查询检测结果，对应 GET /api/detections。 */
    PageResult<DetectionResultVO> pageDetectionResults(
            Long taskId,
            Long imageId,
            String className,
            String status,
            Long pageNo,
            Long pageSize);

    /** 查询检测结果基础详情，对应 GET /api/detections/{id}。 */
    DetectionResultVO getDetectionResultDetail(Long id);

    /** 查询图片与 bbox 组合详情，对应 GET /api/detections/{id}/visual-detail。 */
    DetectionVisualDetailVO getDetectionVisualDetail(Long id);
}
