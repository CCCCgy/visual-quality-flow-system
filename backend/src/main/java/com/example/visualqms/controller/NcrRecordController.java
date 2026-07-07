package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.NcrCreateDTO;
import com.example.visualqms.dto.NcrStatusUpdateDTO;
import com.example.visualqms.service.NcrRecordService;
import com.example.visualqms.vo.NcrRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ncrs")
public class NcrRecordController {

    private final NcrRecordService ncrRecordService;

    public NcrRecordController(NcrRecordService ncrRecordService) {
        this.ncrRecordService = ncrRecordService;
    }

    @PostMapping
    public Result<NcrRecordVO> createNcr(@Valid @RequestBody NcrCreateDTO dto) {
        return Result.success(ncrRecordService.createNcr(dto));
    }

    @GetMapping
    public Result<PageResult<NcrRecordVO>> pageNcrs(
            @RequestParam(required = false) String ncrNo,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(ncrRecordService.pageNcrs(
                ncrNo,
                batchId,
                taskId,
                severity,
                status,
                pageNo,
                pageSize));
    }

    @GetMapping("/{id}")
    public Result<NcrRecordVO> getNcrDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(ncrRecordService.getNcrDetail(id));
    }

    @GetMapping("/by-review/{reviewId}")
    public Result<NcrRecordVO> getNcrByReview(
            @PathVariable @NotNull(message = "reviewId cannot be null") Long reviewId) {
        return Result.success(ncrRecordService.getNcrByReview(reviewId));
    }

    @PatchMapping("/{id}/status")
    public Result<NcrRecordVO> updateNcrStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody NcrStatusUpdateDTO dto) {
        return Result.success(ncrRecordService.updateNcrStatus(id, dto));
    }
}
