package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.CapaCreateDTO;
import com.example.visualqms.dto.CapaStatusUpdateDTO;
import com.example.visualqms.dto.CapaUpdateDTO;
import com.example.visualqms.service.CapaRecordService;
import com.example.visualqms.vo.CapaRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/capas")
public class CapaRecordController {

    private final CapaRecordService capaRecordService;

    public CapaRecordController(CapaRecordService capaRecordService) {
        this.capaRecordService = capaRecordService;
    }

    @PostMapping
    public Result<CapaRecordVO> createCapa(@Valid @RequestBody CapaCreateDTO dto) {
        return Result.success(capaRecordService.createCapa(dto));
    }

    @GetMapping
    public Result<PageResult<CapaRecordVO>> pageCapas(
            @RequestParam(required = false) String capaNo,
            @RequestParam(required = false) Long ncrId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(capaRecordService.pageCapas(
                capaNo,
                ncrId,
                batchId,
                ownerId,
                status,
                pageNo,
                pageSize));
    }

    @GetMapping("/{id}")
    public Result<CapaRecordVO> getCapaDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(capaRecordService.getCapaDetail(id));
    }

    @GetMapping("/by-ncr/{ncrId}")
    public Result<CapaRecordVO> getCapaByNcr(
            @PathVariable @NotNull(message = "ncrId cannot be null") Long ncrId) {
        return Result.success(capaRecordService.getCapaByNcr(ncrId));
    }

    @PutMapping("/{id}")
    public Result<CapaRecordVO> updateCapa(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody CapaUpdateDTO dto) {
        return Result.success(capaRecordService.updateCapa(id, dto));
    }

    @PatchMapping("/{id}/status")
    public Result<CapaRecordVO> updateCapaStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody CapaStatusUpdateDTO dto) {
        return Result.success(capaRecordService.updateCapaStatus(id, dto));
    }
}
