package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.CapaCreateDTO;
import com.example.visualqms.dto.CapaStatusUpdateDTO;
import com.example.visualqms.dto.CapaUpdateDTO;
import com.example.visualqms.entity.CapaRecord;
import com.example.visualqms.vo.CapaRecordVO;

/**
 * 文件职责：
 * 定义 CAPA 整改闭环模块的业务能力。
 *
 * 所属层级：
 * Service。
 *
 * 上游调用：
 * CapaRecordController。
 *
 * 下游依赖：
 * 由 CapaRecordServiceImpl 实现，访问 capa_record、ncr_record、production_batch 和 sys_user。
 *
 * 设计说明：
 * CAPA 状态变化会影响 NCR 和批次，Service 接口用于集中事务和状态流转规则。
 */
public interface CapaRecordService extends IService<CapaRecord> {

    /** 创建 CAPA，对应 POST /api/capas。 */
    CapaRecordVO createCapa(CapaCreateDTO dto);

    /** 分页查询 CAPA，对应 GET /api/capas。 */
    PageResult<CapaRecordVO> pageCapas(
            String capaNo,
            Long ncrId,
            Long batchId,
            Long ownerId,
            String status,
            Long pageNo,
            Long pageSize);

    /** 查询 CAPA 详情，对应 GET /api/capas/{id}。 */
    CapaRecordVO getCapaDetail(Long id);

    /** 根据 NCR 查询 CAPA，对应 GET /api/capas/by-ncr/{ncrId}。 */
    CapaRecordVO getCapaByNcr(Long ncrId);

    /** 更新 CAPA 内容，对应 PUT /api/capas/{id}。 */
    CapaRecordVO updateCapa(Long id, CapaUpdateDTO dto);

    /** 更新 CAPA 状态，对应 PATCH /api/capas/{id}/status。 */
    CapaRecordVO updateCapaStatus(Long id, CapaStatusUpdateDTO dto);
}
