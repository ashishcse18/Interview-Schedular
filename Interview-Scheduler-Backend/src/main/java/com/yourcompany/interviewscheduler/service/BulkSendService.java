package com.yourcompany.interviewscheduler.service;

import com.yourcompany.interviewscheduler.model.entity.Batch;
import com.yourcompany.interviewscheduler.model.entity.Candidate;
import com.yourcompany.interviewscheduler.model.entity.MessageLog;
import com.yourcompany.interviewscheduler.model.entity.MessageTemplate;
import com.yourcompany.interviewscheduler.repository.BatchRepository;
import com.yourcompany.interviewscheduler.repository.CandidateRepository;
import com.yourcompany.interviewscheduler.repository.MessageLogRepository;
import com.yourcompany.interviewscheduler.repository.TemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class BulkSendService {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private MessageLogRepository messageLogRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private TemplateService templateService;

    /**
     * Triggers asynchronous bulk sending of messages to candidates in a batch.
     */
    @Async("bulkSendTaskExecutor")
    public void sendBulkMessages(Long batchId, Long templateId) {
        log.info("Starting async bulk send for batchId: {} and templateId: {}", batchId, templateId);

        Batch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            log.error("Batch with ID {} not found. Aborting sending.", batchId);
            return;
        }

        MessageTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template == null) {
            log.error("Template with ID {} not found. Aborting sending.", templateId);
            batch.setStatus("FAILED");
            batchRepository.save(batch);
            return;
        }

        batch.setStatus("SENDING");
        batchRepository.save(batch);

        List<Candidate> candidates = candidateRepository.findByBatchId(batchId);
        log.info("Found {} candidates in batch {}", candidates.size(), batchId);

        for (Candidate candidate : candidates) {
            // Check if log already exists to avoid sending duplicates (in case of retry/resume)
            MessageLog messageLog = messageLogRepository.findByCandidateIdAndTemplateId(candidate.getId(), templateId)
                    .orElseGet(() -> MessageLog.builder()
                            .batchId(batchId)
                            .candidateId(candidate.getId())
                            .templateId(templateId)
                            .status("PENDING")
                            .build());

            // If already successfully sent, skip it
            if ("SENT".equals(messageLog.getStatus()) || "DELIVERED".equals(messageLog.getStatus())) {
                continue;
            }

            try {
                // Resolve template content for this candidate
                String messageText = templateService.resolveTemplate(template.getContent(), candidate);

                // Call WhatsApp sending service
                String messageId = whatsAppService.sendMessage(candidate.getWhatsappNumber(), messageText, template.getName(), candidate);

                messageLog.setStatus("SENT");
                messageLog.setWhatsappMessageId(messageId);
                messageLog.setSentAt(LocalDateTime.now());
                messageLog.setErrorMessage(null);
                log.info("Successfully sent message to candidate ID: {} with SID: {}", candidate.getId(), messageId);
            } catch (Exception e) {
                log.error("Failed to send message to candidate ID: {}. Error: {}", candidate.getId(), e.getMessage());
                messageLog.setStatus("FAILED");
                messageLog.setErrorMessage(e.getMessage());
            }

            messageLogRepository.save(messageLog);

            // Rate Pacing: Wait for 300ms between sends to avoid rate limit bans
            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Bulk sending thread interrupted.");
                break;
            }
        }

        batch.setStatus("COMPLETED");
        batchRepository.save(batch);
        log.info("Bulk sending completed for batchId: {}", batchId);
    }
}
