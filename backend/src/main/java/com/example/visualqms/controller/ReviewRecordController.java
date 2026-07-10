package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.ReviewCreateDTO;
import com.example.visualqms.service.ReviewRecordService;
import com.example.visualqms.vo.ReviewRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件职责：
 * 提供人工复核记录的创建、分页查询、详情和按检测结果查询接口。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * reviewApi.js 被 DetectionResultListView.vue、DetectionResultDetailView.vue 和 ReviewListView.vue 使用。
 *
 * 下游依赖：
 * 调用 ReviewRecordService，由 Service 校验检测结果是否待复核、复核人是否存在、是否已复核。
 *
 * 主要业务链路：
 * DetectionResultListView.vue 或 DetectionResultDetailView.vue -> reviewApi.js
 * -> POST /api/reviews -> ReviewRecordController -> ReviewRecordService
 * -> ReviewRecordServiceImpl -> ReviewRecordMapper / DetectionResultMapper
 * -> review_record / detection_result。
 *
 * 注意事项：
 * 人工结论写入 review_record，同时同步 detection_result.status；该一致性由事务性 Service 保障。
 */
@Validated
@RestController
@RequestMapping("/api/reviews")
public class ReviewRecordController {

    private final ReviewRecordService reviewRecordService;

    public ReviewRecordController(ReviewRecordService reviewRecordService) {
        this.reviewRecordService = reviewRecordService;
    }

    /**
     * 创建人工复核记录。
     *
     * @param dto 检测结果 ID、复核人 ID、复核结论和备注
     * @return 创建后的复核记录
     */
    @PostMapping
    public Result<ReviewRecordVO> createReview(@Valid @RequestBody ReviewCreateDTO dto) {
        return Result.success(reviewRecordService.createReview(dto));
    }

    /**
     * 分页查询人工复核记录。
     *
     * @param taskId 检测任务 ID 过滤条件
     * @param imageId 图片 ID 过滤条件
     * @param reviewerId 复核人 ID 过滤条件
     * @param reviewResult 复核结论过滤条件
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 复核记录分页结果
     */
    @GetMapping
    public Result<PageResult<ReviewRecordVO>> pageReviews(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long imageId,
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) String reviewResult,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(reviewRecordService.pageReviews(
                taskId,
                imageId,
                reviewerId,
                reviewResult,
                pageNo,
                pageSize));
    }

    /**
     * 查询单条复核记录。
     *
     * @param id review_record 主键
     * @return 复核记录详情
     */
    @GetMapping("/{id}")
    public Result<ReviewRecordVO> getReviewDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(reviewRecordService.getReviewDetail(id));
    }

    /**
     * 根据检测结果查询复核记录，用于判断某条 detection_result 是否已产生人工结论。
     *
     * @param detectionResultId detection_result 主键
     * @return 对应复核记录；不存在时返回 null
     */
    @GetMapping("/by-detection/{detectionResultId}")
    public Result<ReviewRecordVO> getReviewByDetectionResult(
            @PathVariable @NotNull(message = "detectionResultId cannot be null") Long detectionResultId) {
        return Result.success(reviewRecordService.getReviewByDetectionResult(detectionResultId));
    }
}
