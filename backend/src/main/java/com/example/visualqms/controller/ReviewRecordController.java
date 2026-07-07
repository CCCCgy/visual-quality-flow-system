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

@Validated
@RestController
@RequestMapping("/api/reviews")
public class ReviewRecordController {

    private final ReviewRecordService reviewRecordService;

    public ReviewRecordController(ReviewRecordService reviewRecordService) {
        this.reviewRecordService = reviewRecordService;
    }

    @PostMapping
    public Result<ReviewRecordVO> createReview(@Valid @RequestBody ReviewCreateDTO dto) {
        return Result.success(reviewRecordService.createReview(dto));
    }

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

    @GetMapping("/{id}")
    public Result<ReviewRecordVO> getReviewDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(reviewRecordService.getReviewDetail(id));
    }

    @GetMapping("/by-detection/{detectionResultId}")
    public Result<ReviewRecordVO> getReviewByDetectionResult(
            @PathVariable @NotNull(message = "detectionResultId cannot be null") Long detectionResultId) {
        return Result.success(reviewRecordService.getReviewByDetectionResult(detectionResultId));
    }
}
