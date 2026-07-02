package com.yourcompany.interviewscheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDTO {
    private Long id;
    private Long batchId;
    private String name;
    private String email;
    private String whatsappNumber;
    private String role;
    private String companyName;
    private LocalDateTime panelTiming;
    private String gmeetLink;
    private String interviewerName;
}
