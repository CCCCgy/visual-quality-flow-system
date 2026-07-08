package com.example.visualqms.controller;

import com.example.visualqms.common.Result;
import com.example.visualqms.service.DashboardService;
import com.example.visualqms.vo.ClassCountVO;
import com.example.visualqms.vo.DashboardSummaryVO;
import com.example.visualqms.vo.StatusCountVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> getDashboardSummary() {
        return Result.success(dashboardService.getSummary());
    }

    @GetMapping("/batch-status")
    public Result<List<StatusCountVO>> getBatchStatusStats() {
        return Result.success(dashboardService.getBatchStatusStats());
    }

    @GetMapping("/detection-status")
    public Result<List<StatusCountVO>> getDetectionStatusStats() {
        return Result.success(dashboardService.getDetectionStatusStats());
    }

    @GetMapping("/defect-class")
    public Result<List<ClassCountVO>> getDefectClassStats() {
        return Result.success(dashboardService.getDefectClassStats());
    }

    @GetMapping("/ncr-status")
    public Result<List<StatusCountVO>> getNcrStatusStats() {
        return Result.success(dashboardService.getNcrStatusStats());
    }

    @GetMapping("/capa-status")
    public Result<List<StatusCountVO>> getCapaStatusStats() {
        return Result.success(dashboardService.getCapaStatusStats());
    }
}
