package com.example.visualqms.vo;

import lombok.Data;

@Data
public class DashboardSummaryVO {

    private Long batchCount;

    private Long taskCount;

    private Long detectionCount;

    private Long pendingReviewCount;

    private Long confirmedDefectCount;

    private Long openNcrCount;

    private Long inProgressCapaCount;

    private Long closedCapaCount;
}
