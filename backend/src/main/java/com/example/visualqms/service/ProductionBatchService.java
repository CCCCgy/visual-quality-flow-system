package com.example.visualqms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.visualqms.common.PageResult;
import com.example.visualqms.dto.ProductionBatchCreateDTO;
import com.example.visualqms.dto.ProductionBatchStatusUpdateDTO;
import com.example.visualqms.dto.ProductionBatchUpdateDTO;
import com.example.visualqms.entity.ProductionBatch;
import com.example.visualqms.vo.ProductionBatchVO;

public interface ProductionBatchService extends IService<ProductionBatch> {

    ProductionBatchVO createBatch(ProductionBatchCreateDTO dto);

    PageResult<ProductionBatchVO> pageBatches(String batchNo, String status, Long pageNo, Long pageSize);

    ProductionBatchVO getBatchDetail(Long id);

    ProductionBatchVO updateBatch(Long id, ProductionBatchUpdateDTO dto);

    ProductionBatchVO updateBatchStatus(Long id, ProductionBatchStatusUpdateDTO dto);
}
