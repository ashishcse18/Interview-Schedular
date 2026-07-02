package com.yourcompany.interviewscheduler.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendRequest {

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @NotNull(message = "Template ID is required")
    private Long templateId;
}
