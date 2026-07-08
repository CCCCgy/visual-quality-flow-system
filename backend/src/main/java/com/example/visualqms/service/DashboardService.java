package com.example.visualqms.service;

import com.example.visualqms.vo.ClassCountVO;
import com.example.visualqms.vo.DashboardSummaryVO;
import com.example.visualqms.vo.StatusCountVO;
import java.util.List;

public interface DashboardService {

    DashboardSummaryVO getSummary();

    List<StatusCountVO> getBatchStatusStats();

    List<StatusCountVO> getDetectionStatusStats();

    List<ClassCountVO> getDefectClassStats();

    List<StatusCountVO> getNcrStatusStats();

    List<StatusCountVO> getCapaStatusStats();
}
