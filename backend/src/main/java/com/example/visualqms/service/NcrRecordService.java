package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.NcrCreateDTO;
import com.example.visualqms.dto.NcrStatusUpdateDTO;
import com.example.visualqms.entity.NcrRecord;
import com.example.visualqms.vo.NcrRecordVO;

public interface NcrRecordService extends IService<NcrRecord> {

    NcrRecordVO createNcr(NcrCreateDTO dto);

    PageResult<NcrRecordVO> pageNcrs(
            String ncrNo,
            Long batchId,
            Long taskId,
            String severity,
            String status,
            Long pageNo,
            Long pageSize);

    NcrRecordVO getNcrDetail(Long id);

    NcrRecordVO getNcrByReview(Long reviewId);

    NcrRecordVO updateNcrStatus(Long id, NcrStatusUpdateDTO dto);
}
