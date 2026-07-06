package com.example.visualqms.service;

import com.example.visualqms.dto.DetectionImportDTO;
import com.example.visualqms.vo.DetectionImportResultVO;

public interface DetectionImportService {

    DetectionImportResultVO importYoloJson(DetectionImportDTO dto);
}
