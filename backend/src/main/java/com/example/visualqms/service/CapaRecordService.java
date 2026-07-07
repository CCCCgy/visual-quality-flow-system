package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.CapaCreateDTO;
import com.example.visualqms.dto.CapaStatusUpdateDTO;
import com.example.visualqms.dto.CapaUpdateDTO;
import com.example.visualqms.entity.CapaRecord;
import com.example.visualqms.vo.CapaRecordVO;

public interface CapaRecordService extends IService<CapaRecord> {

    CapaRecordVO createCapa(CapaCreateDTO dto);

    PageResult<CapaRecordVO> pageCapas(
            String capaNo,
            Long ncrId,
            Long batchId,
            Long ownerId,
            String status,
            Long pageNo,
            Long pageSize);

    CapaRecordVO getCapaDetail(Long id);

    CapaRecordVO getCapaByNcr(Long ncrId);

    CapaRecordVO updateCapa(Long id, CapaUpdateDTO dto);

    CapaRecordVO updateCapaStatus(Long id, CapaStatusUpdateDTO dto);
}
