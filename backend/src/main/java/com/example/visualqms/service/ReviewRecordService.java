package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.ReviewCreateDTO;
import com.example.visualqms.entity.ReviewRecord;
import com.example.visualqms.vo.ReviewRecordVO;

/**
 * 文件职责：
 * 定义人工复核模块的业务能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * ReviewRecordController。
 *
 * 下游依赖：
 * 由 ReviewRecordServiceImpl 实现，访问 review_record、detection_result 和 sys_user。
 *
 * 设计说明：
 * 人工复核会同时新增复核记录并同步检测结果状态，必须通过 Service 管理事务。
 */
public interface ReviewRecordService extends IService<ReviewRecord> {

    /** 创建人工复核记录，对应 POST /api/reviews。 */
    ReviewRecordVO createReview(ReviewCreateDTO dto);

    /** 分页查询复核记录，对应 GET /api/reviews。 */
    PageResult<ReviewRecordVO> pageReviews(
            Long taskId,
            Long imageId,
            Long reviewerId,
            String reviewResult,
            Long pageNo,
            Long pageSize);

    /** 查询复核详情，对应 GET /api/reviews/{id}。 */
    ReviewRecordVO getReviewDetail(Long id);

    /** 根据检测结果查询复核记录，对应 GET /api/reviews/by-detection/{detectionResultId}。 */
    ReviewRecordVO getReviewByDetectionResult(Long detectionResultId);
}
