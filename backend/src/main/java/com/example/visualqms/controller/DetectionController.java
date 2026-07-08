package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.DetectionImportDTO;
import com.example.visualqms.service.DetectionImportService;
import com.example.visualqms.service.DetectionResultService;
import com.example.visualqms.vo.DetectionImportResultVO;
import com.example.visualqms.vo.DetectionResultVO;
import com.example.visualqms.vo.DetectionVisualDetailVO;
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
@RequestMapping("/api/detections")
public class DetectionController {

    private final DetectionImportService detectionImportService;
    private final DetectionResultService detectionResultService;

    public DetectionController(
            DetectionImportService detectionImportService,
            DetectionResultService detectionResultService) {
        this.detectionImportService = detectionImportService;
        this.detectionResultService = detectionResultService;
    }

    @PostMapping("/import-json")
    public Result<DetectionImportResultVO> importYoloJson(@Valid @RequestBody DetectionImportDTO dto) {
        return Result.success(detectionImportService.importYoloJson(dto));
    }

    @GetMapping
    public Result<PageResult<DetectionResultVO>> pageDetectionResults(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long imageId,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(detectionResultService.pageDetectionResults(
                taskId,
                imageId,
                className,
                status,
                pageNo,
                pageSize));
    }

    @GetMapping("/{id}")
    public Result<DetectionResultVO> getDetectionResultDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(detectionResultService.getDetectionResultDetail(id));
    }

    @GetMapping("/{id}/visual-detail")
    public Result<DetectionVisualDetailVO> getDetectionVisualDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(detectionResultService.getDetectionVisualDetail(id));
    }
}
