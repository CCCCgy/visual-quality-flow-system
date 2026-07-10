package com.example.visualqms.service;

import com.example.visualqms.vo.ClassCountVO;
import com.example.visualqms.vo.DashboardSummaryVO;
import com.example.visualqms.vo.StatusCountVO;
import java.util.List;

/**
 * 文件职责：
 * 定义 Dashboard 首页统计能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * DashboardController。
 *
 * 下游依赖：
 * 由 DashboardServiceImpl 实现，统计 production_batch、inspection_task、detection_result、ncr_record 和 capa_record。
 *
 * 设计说明：
 * Dashboard 返回的是面向图表的 VO，而不是单表 Entity。
 */
public interface DashboardService {

    /** 查询顶部汇总卡片。 */
    DashboardSummaryVO getSummary();

    /** 查询批次状态分布。 */
    List<StatusCountVO> getBatchStatusStats();

    /** 查询检测结果状态分布。 */
    List<StatusCountVO> getDetectionStatusStats();

    /** 查询缺陷类别分布。 */
    List<ClassCountVO> getDefectClassStats();

    /** 查询 NCR 状态分布。 */
    List<StatusCountVO> getNcrStatusStats();

    /** 查询 CAPA 状态分布。 */
    List<StatusCountVO> getCapaStatusStats();
}
