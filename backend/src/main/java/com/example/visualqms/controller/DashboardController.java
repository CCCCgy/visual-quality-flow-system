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

/**
 * 文件职责：
 * 提供首页 Dashboard 的汇总指标和图表统计接口。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * DashboardView.vue -> dashboardApi.js。
 *
 * 下游依赖：
 * 调用 DashboardService，最终通过各 Mapper 统计 production_batch、inspection_task、
 * detection_result、ncr_record、capa_record。
 *
 * 主要业务链路：
 * DashboardView.vue -> dashboardApi.js -> GET /api/dashboard/*
 * -> DashboardController -> DashboardService -> DashboardServiceImpl
 * -> 对应 Mapper -> VO -> ECharts。
 *
 * 注意事项：
 * 本层只暴露统计接口，具体 count/group by 含义集中在 DashboardServiceImpl。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询看板顶部汇总卡片数据。
     *
     * @return DashboardSummaryVO，包含批次、任务、检测结果、待复核、确认缺陷、OPEN NCR 和 CAPA 数量
     */
    @GetMapping("/summary")
    public Result<DashboardSummaryVO> getDashboardSummary() {
        return Result.success(dashboardService.getSummary());
    }

    /**
     * 按批次状态统计数量，用于批次状态饼图。
     */
    @GetMapping("/batch-status")
    public Result<List<StatusCountVO>> getBatchStatusStats() {
        return Result.success(dashboardService.getBatchStatusStats());
    }

    /**
     * 按检测结果状态统计数量，用于检测状态柱状图。
     */
    @GetMapping("/detection-status")
    public Result<List<StatusCountVO>> getDetectionStatusStats() {
        return Result.success(dashboardService.getDetectionStatusStats());
    }

    /**
     * 按缺陷类别统计数量，用于缺陷类别柱状图。
     */
    @GetMapping("/defect-class")
    public Result<List<ClassCountVO>> getDefectClassStats() {
        return Result.success(dashboardService.getDefectClassStats());
    }

    /**
     * 按 NCR 状态统计数量，用于 NCR 状态饼图。
     */
    @GetMapping("/ncr-status")
    public Result<List<StatusCountVO>> getNcrStatusStats() {
        return Result.success(dashboardService.getNcrStatusStats());
    }

    /**
     * 按 CAPA 状态统计数量，用于 CAPA 状态饼图。
     */
    @GetMapping("/capa-status")
    public Result<List<StatusCountVO>> getCapaStatusStats() {
        return Result.success(dashboardService.getCapaStatusStats());
    }
}
