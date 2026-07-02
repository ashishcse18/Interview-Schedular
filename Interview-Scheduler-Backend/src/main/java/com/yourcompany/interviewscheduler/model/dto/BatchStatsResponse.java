package com.yourcompany.interviewscheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatsResponse {
    private Long batchId;
    private String fileName;
    private String status;
    private Integer totalCandidates;
    private Long pendingCount;
    private Long sentCount;
    private Long failedCount;
    private Long deliveredCount;
}
