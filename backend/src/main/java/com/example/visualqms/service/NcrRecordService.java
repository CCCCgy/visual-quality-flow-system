package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.NcrCreateDTO;
import com.example.visualqms.dto.NcrStatusUpdateDTO;
import com.example.visualqms.entity.NcrRecord;
import com.example.visualqms.vo.NcrRecordVO;

/**
 * 文件职责：
 * 定义 NCR 不合格记录模块的业务能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * NcrRecordController。
 *
 * 下游依赖：
 * 由 NcrRecordServiceImpl 实现，访问 review_record、detection_result、inspection_task、ncr_record 和 production_batch。
 *
 * 设计说明：
 * NCR 创建需要跨表追溯确认缺陷来源并推进批次状态，因此 Controller 不能直接操作 NcrRecordMapper。
 */
public interface NcrRecordService extends IService<NcrRecord> {

    /** 创建 NCR，对应 POST /api/ncrs。 */
    NcrRecordVO createNcr(NcrCreateDTO dto);

    /** 分页查询 NCR，对应 GET /api/ncrs。 */
    PageResult<NcrRecordVO> pageNcrs(
            String ncrNo,
            Long batchId,
            Long taskId,
            String severity,
            String status,
            Long pageNo,
            Long pageSize);

    /** 查询 NCR 详情，对应 GET /api/ncrs/{id}。 */
    NcrRecordVO getNcrDetail(Long id);

    /** 根据复核记录查询 NCR，对应 GET /api/ncrs/by-review/{reviewId}。 */
    NcrRecordVO getNcrByReview(Long reviewId);

    /** 更新 NCR 状态，对应 PATCH /api/ncrs/{id}/status。 */
    NcrRecordVO updateNcrStatus(Long id, NcrStatusUpdateDTO dto);
}
