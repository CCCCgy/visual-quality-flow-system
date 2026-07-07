package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.ReviewCreateDTO;
import com.example.visualqms.entity.ReviewRecord;
import com.example.visualqms.vo.ReviewRecordVO;

public interface ReviewRecordService extends IService<ReviewRecord> {

    ReviewRecordVO createReview(ReviewCreateDTO dto);

    PageResult<ReviewRecordVO> pageReviews(
            Long taskId,
            Long imageId,
            Long reviewerId,
            String reviewResult,
            Long pageNo,
            Long pageSize);

    ReviewRecordVO getReviewDetail(Long id);

    ReviewRecordVO getReviewByDetectionResult(Long detectionResultId);
}
