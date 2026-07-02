package com.yourcompany.interviewscheduler.service;

import com.yourcompany.interviewscheduler.model.entity.Candidate;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TemplateServiceTest {

    private final TemplateService templateService = new TemplateService();

    @Test
    void testResolveTemplate() {
        Candidate candidate = Candidate.builder()
                .name("John Doe")
                .companyName("TechCorp")
                .role("Java Developer")
                .panelTiming(LocalDateTime.of(2026, 6, 20, 10, 0))
                .gmeetLink("https://meet.google.com/abc-xyz")
                .interviewerName("Mr. Smith")
                .build();

        String template = "Hello {{candidateName}}, shortlist at {{companyName}} for {{role}} on {{panelTiming}}. Link: {{gmeetLink}} interviewer: {{interviewerName}}.";
        String expected = "Hello John Doe, shortlist at TechCorp for Java Developer on 20 Jun 2026, 10:00 AM. Link: https://meet.google.com/abc-xyz interviewer: Mr. Smith.";

        String resolved = templateService.resolveTemplate(template, candidate);
        assertTrue(expected.equalsIgnoreCase(resolved), "Expected: " + expected + "\nBut got: " + resolved);
    }
}
