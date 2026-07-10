package com.example.visualqms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.visualqms.entity.CapaRecord;
import com.example.visualqms.entity.DetectionResult;
import com.example.visualqms.entity.InspectionTask;
import com.example.visualqms.entity.NcrRecord;
import com.example.visualqms.entity.ProductionBatch;
import com.example.visualqms.mapper.CapaRecordMapper;
import com.example.visualqms.mapper.DetectionResultMapper;
import com.example.visualqms.mapper.InspectionTaskMapper;
import com.example.visualqms.mapper.NcrRecordMapper;
import com.example.visualqms.mapper.ProductionBatchMapper;
import com.example.visualqms.service.DashboardService;
import com.example.visualqms.vo.ClassCountVO;
import com.example.visualqms.vo.DashboardSummaryVO;
import com.example.visualqms.vo.StatusCountVO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 文件职责：
 * 汇总 Dashboard 页面所需的数量指标和分组统计。
 *
 * 所属层级：
 * ServiceImpl。
 *
 * 上游调用：
 * DashboardController。
 *
 * 下游依赖：
 * ProductionBatchMapper、InspectionTaskMapper、DetectionResultMapper、NcrRecordMapper、CapaRecordMapper。
 *
 * 主要业务链路：
 * DashboardView.vue -> dashboardApi.js -> DashboardController -> DashboardServiceImpl
 * -> production_batch / inspection_task / detection_result / ncr_record / capa_record
 * -> DashboardSummaryVO / StatusCountVO / ClassCountVO -> ECharts。
 *
 * 注意事项：
 * 本类只做只读统计，不修改任何状态。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String DETECTION_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String DETECTION_STATUS_CONFIRMED_DEFECT = "CONFIRMED_DEFECT";
    private static final String NCR_STATUS_OPEN = "OPEN";
    private static final String CAPA_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String CAPA_STATUS_CLOSED = "CLOSED";

    private final ProductionBatchMapper productionBatchMapper;
    private final InspectionTaskMapper inspectionTaskMapper;
    private final DetectionResultMapper detectionResultMapper;
    private final NcrRecordMapper ncrRecordMapper;
    private final CapaRecordMapper capaRecordMapper;

    public DashboardServiceImpl(
            ProductionBatchMapper productionBatchMapper,
            InspectionTaskMapper inspectionTaskMapper,
            DetectionResultMapper detectionResultMapper,
            NcrRecordMapper ncrRecordMapper,
            CapaRecordMapper capaRecordMapper) {
        this.productionBatchMapper = productionBatchMapper;
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.detectionResultMapper = detectionResultMapper;
        this.ncrRecordMapper = ncrRecordMapper;
        this.capaRecordMapper = capaRecordMapper;
    }

    /**
     * 查询看板汇总卡片。
     *
     * 数据来源：
     * production_batch 总数、inspection_task 总数、detection_result 总数；
     * detection_result 中 PENDING_REVIEW 和 CONFIRMED_DEFECT 数量；
     * ncr_record 中 OPEN 数量；
     * capa_record 中 IN_PROGRESS 和 CLOSED 数量。
     */
    @Override
    public DashboardSummaryVO getSummary() {
        DashboardSummaryVO summary = new DashboardSummaryVO();
        summary.setBatchCount(countAll(productionBatchMapper));
        summary.setTaskCount(countAll(inspectionTaskMapper));
        summary.setDetectionCount(countAll(detectionResultMapper));
        summary.setPendingReviewCount(countDetectionByStatus(DETECTION_STATUS_PENDING_REVIEW));
        summary.setConfirmedDefectCount(countDetectionByStatus(DETECTION_STATUS_CONFIRMED_DEFECT));
        summary.setOpenNcrCount(countNcrByStatus(NCR_STATUS_OPEN));
        summary.setInProgressCapaCount(countCapaByStatus(CAPA_STATUS_IN_PROGRESS));
        summary.setClosedCapaCount(countCapaByStatus(CAPA_STATUS_CLOSED));
        return summary;
    }

    /**
     * 按 production_batch.status 分组统计，供批次状态饼图使用。
     */
    @Override
    public List<StatusCountVO> getBatchStatusStats() {
        return queryStatusStats(productionBatchMapper, new QueryWrapper<ProductionBatch>());
    }

    /**
     * 按 detection_result.status 分组统计，供检测结果状态图使用。
     */
    @Override
    public List<StatusCountVO> getDetectionStatusStats() {
        return queryStatusStats(detectionResultMapper, new QueryWrapper<DetectionResult>());
    }

    /**
     * 按 detection_result.class_name 分组统计缺陷类别数量。
     */
    @Override
    public List<ClassCountVO> getDefectClassStats() {
        QueryWrapper<DetectionResult> queryWrapper = new QueryWrapper<DetectionResult>()
                .select("class_name", "COUNT(*) AS count")
                .groupBy("class_name")
                .orderByDesc("count");

        return detectionResultMapper.selectMaps(queryWrapper)
                .stream()
                .map(this::toClassCountVO)
                .toList();
    }

    /**
     * 按 ncr_record.status 分组统计，供 NCR 状态饼图使用。
     */
    @Override
    public List<StatusCountVO> getNcrStatusStats() {
        return queryStatusStats(ncrRecordMapper, new QueryWrapper<NcrRecord>());
    }

    /**
     * 按 capa_record.status 分组统计，供 CAPA 状态饼图使用。
     */
    @Override
    public List<StatusCountVO> getCapaStatusStats() {
        return queryStatusStats(capaRecordMapper, new QueryWrapper<CapaRecord>());
    }

    /**
     * 统计某检测结果状态的数量。
     */
    private Long countDetectionByStatus(String status) {
        LambdaQueryWrapper<DetectionResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DetectionResult::getStatus, status);
        return detectionResultMapper.selectCount(queryWrapper);
    }

    /**
     * 统计某 NCR 状态的数量。
     */
    private Long countNcrByStatus(String status) {
        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getStatus, status);
        return ncrRecordMapper.selectCount(queryWrapper);
    }

    /**
     * 统计某 CAPA 状态的数量。
     */
    private Long countCapaByStatus(String status) {
        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getStatus, status);
        return capaRecordMapper.selectCount(queryWrapper);
    }

    /**
     * 统计 Mapper 对应表的总行数。
     */
    private <T> Long countAll(BaseMapper<T> mapper) {
        return mapper.selectCount(null);
    }

    /**
     * 通用 status group by 统计，要求目标表存在 status 字段。
     */
    private <T> List<StatusCountVO> queryStatusStats(BaseMapper<T> mapper, QueryWrapper<T> queryWrapper) {
        queryWrapper.select("status", "COUNT(*) AS count")
                .groupBy("status")
                .orderByDesc("count");

        return mapper.selectMaps(queryWrapper)
                .stream()
                .map(this::toStatusCountVO)
                .toList();
    }

    /**
     * 将 MyBatis-Plus selectMaps 返回的 status/count Map 转为前端 VO。
     */
    private StatusCountVO toStatusCountVO(Map<String, Object> map) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus((String) map.get("status"));
        vo.setCount(toLong(map.get("count")));
        return vo;
    }

    /**
     * 将 class_name/count Map 转为缺陷类别统计 VO。
     */
    private ClassCountVO toClassCountVO(Map<String, Object> map) {
        ClassCountVO vo = new ClassCountVO();
        vo.setClassName((String) map.get("class_name"));
        vo.setCount(toLong(map.get("count")));
        return vo;
    }

    /**
     * 兼容不同 JDBC 驱动返回的数字类型。
     */
    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
