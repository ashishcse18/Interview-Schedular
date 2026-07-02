package com.yourcompany.interviewscheduler.service;

import com.yourcompany.interviewscheduler.model.entity.Candidate;
import com.yourcompany.interviewscheduler.model.entity.MessageTemplate;
import com.yourcompany.interviewscheduler.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    private static final DateTimeFormatter FRIENDLY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public MessageTemplate createTemplate(MessageTemplate template) {
        return templateRepository.save(template);
    }

    public List<MessageTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    public Optional<MessageTemplate> getTemplateById(Long id) {
        return templateRepository.findById(id);
    }

    public MessageTemplate updateTemplate(Long id, MessageTemplate updated) {
        return templateRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setContent(updated.getContent());
                    return templateRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Template with ID " + id + " not found"));
    }

    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }

    public String resolveTemplate(String templateContent, Candidate candidate) {
        if (templateContent == null || candidate == null) {
            return "";
        }

        String resolved = templateContent;

        String timingStr = candidate.getPanelTiming() != null
                ? candidate.getPanelTiming().format(FRIENDLY_DATE_FORMAT)
                : "";
        String interviewer = candidate.getInterviewerName() != null && !candidate.getInterviewerName().trim().isEmpty()
                ? candidate.getInterviewerName()
                : "HR Team";

        resolved = resolved.replace("{{candidateName}}", emptyIfNull(candidate.getName()));
        resolved = resolved.replace("{{companyName}}", emptyIfNull(candidate.getCompanyName()));
        resolved = resolved.replace("{{role}}", emptyIfNull(candidate.getRole()));
        resolved = resolved.replace("{{panelTiming}}", timingStr);
        resolved = resolved.replace("{{gmeetLink}}", emptyIfNull(candidate.getGmeetLink()));
        resolved = resolved.replace("{{interviewerName}}", interviewer);

        return resolved;
    }

    private String emptyIfNull(String str) {
        return str == null ? "" : str;
    }
}
