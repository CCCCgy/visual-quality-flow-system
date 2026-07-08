package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.entity.DetectionResult;
import com.example.visualqms.vo.DetectionResultVO;
import com.example.visualqms.vo.DetectionVisualDetailVO;

public interface DetectionResultService extends IService<DetectionResult> {

    PageResult<DetectionResultVO> pageDetectionResults(
            Long taskId,
            Long imageId,
            String className,
            String status,
            Long pageNo,
            Long pageSize);

    DetectionResultVO getDetectionResultDetail(Long id);

    DetectionVisualDetailVO getDetectionVisualDetail(Long id);
}
