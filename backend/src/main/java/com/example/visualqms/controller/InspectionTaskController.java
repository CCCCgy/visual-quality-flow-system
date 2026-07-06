package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.InspectionTaskCreateDTO;
import com.example.visualqms.dto.InspectionTaskStatusUpdateDTO;
import com.example.visualqms.service.InspectionTaskService;
import com.example.visualqms.vo.InspectionTaskVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
@RequestMapping("/api/inspection-tasks")
public class InspectionTaskController {

    private final InspectionTaskService inspectionTaskService;

    public InspectionTaskController(InspectionTaskService inspectionTaskService) {
        this.inspectionTaskService = inspectionTaskService;
    }

    @PostMapping
    public Result<InspectionTaskVO> createTask(@Valid @RequestBody InspectionTaskCreateDTO dto) {
        return Result.success(inspectionTaskService.createTask(dto));
    }

    @GetMapping
    public Result<PageResult<InspectionTaskVO>> pageTasks(
            @RequestParam(required = false) String taskNo,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(inspectionTaskService.pageTasks(taskNo, batchId, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public Result<InspectionTaskVO> getTaskDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(inspectionTaskService.getTaskDetail(id));
    }

    @GetMapping("/by-batch/{batchId}")
    public Result<List<InspectionTaskVO>> listTasksByBatch(
            @PathVariable @NotNull(message = "batchId cannot be null") Long batchId) {
        return Result.success(inspectionTaskService.listTasksByBatch(batchId));
    }

    @PatchMapping("/{id}/status")
    public Result<InspectionTaskVO> updateTaskStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody InspectionTaskStatusUpdateDTO dto) {
        return Result.success(inspectionTaskService.updateTaskStatus(id, dto));
    }
}
