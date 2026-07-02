package com.yourcompany.interviewscheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResponse {
    private boolean success;
    private String message;
    private Long batchId;
}
