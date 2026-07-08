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

    @Override
    public List<StatusCountVO> getBatchStatusStats() {
        return queryStatusStats(productionBatchMapper, new QueryWrapper<ProductionBatch>());
    }

    @Override
    public List<StatusCountVO> getDetectionStatusStats() {
        return queryStatusStats(detectionResultMapper, new QueryWrapper<DetectionResult>());
    }

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

    @Override
    public List<StatusCountVO> getNcrStatusStats() {
        return queryStatusStats(ncrRecordMapper, new QueryWrapper<NcrRecord>());
    }

    @Override
    public List<StatusCountVO> getCapaStatusStats() {
        return queryStatusStats(capaRecordMapper, new QueryWrapper<CapaRecord>());
    }

    private Long countDetectionByStatus(String status) {
        LambdaQueryWrapper<DetectionResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DetectionResult::getStatus, status);
        return detectionResultMapper.selectCount(queryWrapper);
    }

    private Long countNcrByStatus(String status) {
        LambdaQueryWrapper<NcrRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NcrRecord::getStatus, status);
        return ncrRecordMapper.selectCount(queryWrapper);
    }

    private Long countCapaByStatus(String status) {
        LambdaQueryWrapper<CapaRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CapaRecord::getStatus, status);
        return capaRecordMapper.selectCount(queryWrapper);
    }

    private <T> Long countAll(BaseMapper<T> mapper) {
        return mapper.selectCount(null);
    }

    private <T> List<StatusCountVO> queryStatusStats(BaseMapper<T> mapper, QueryWrapper<T> queryWrapper) {
        queryWrapper.select("status", "COUNT(*) AS count")
                .groupBy("status")
                .orderByDesc("count");

        return mapper.selectMaps(queryWrapper)
                .stream()
                .map(this::toStatusCountVO)
                .toList();
    }

    private StatusCountVO toStatusCountVO(Map<String, Object> map) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus((String) map.get("status"));
        vo.setCount(toLong(map.get("count")));
        return vo;
    }

    private ClassCountVO toClassCountVO(Map<String, Object> map) {
        ClassCountVO vo = new ClassCountVO();
        vo.setClassName((String) map.get("class_name"));
        vo.setCount(toLong(map.get("count")));
        return vo;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
