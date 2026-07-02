package com.yourcompany.interviewscheduler.controller;

import com.yourcompany.interviewscheduler.model.dto.BatchStatsResponse;
import com.yourcompany.interviewscheduler.model.dto.SendRequest;
import com.yourcompany.interviewscheduler.model.dto.SendResponse;
import com.yourcompany.interviewscheduler.model.entity.*;
import com.yourcompany.interviewscheduler.repository.BatchRepository;
import com.yourcompany.interviewscheduler.repository.CandidateRepository;
import com.yourcompany.interviewscheduler.repository.MessageLogRepository;
import com.yourcompany.interviewscheduler.repository.TemplateRepository;
import com.yourcompany.interviewscheduler.service.BulkSendService;
import com.yourcompany.interviewscheduler.service.TemplateService;
import com.yourcompany.interviewscheduler.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
@Tag(name = "Messages", description = "Endpoints for sending WhatsApp messages individually or in bulk, and checking status")
public class MessageController {

    @Autowired
    private BulkSendService bulkSendService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private MessageLogRepository messageLogRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @PostMapping("/send-all")
    @Operation(summary = "Trigger bulk WhatsApp send to all candidates in a batch",
            description = "Initiates an asynchronous background task to send formatted WhatsApp messages to all candidates in the batch using the specified template")
    public ResponseEntity<SendResponse> sendAll(@Valid @RequestBody SendRequest request) {
        if (!batchRepository.existsById(request.getBatchId())) {
            return ResponseEntity.badRequest().body(SendResponse.builder()
                    .success(false)
                    .message("Batch ID " + request.getBatchId() + " does not exist.")
                    .build());
        }

        if (!templateRepository.existsById(request.getTemplateId())) {
            return ResponseEntity.badRequest().body(SendResponse.builder()
                    .success(false)
                    .message("Template ID " + request.getTemplateId() + " does not exist.")
                    .build());
        }

        // Trigger async execution
        bulkSendService.sendBulkMessages(request.getBatchId(), request.getTemplateId());

        return ResponseEntity.ok(SendResponse.builder()
                .success(true)
                .message("Bulk send process initiated successfully in the background.")
                .batchId(request.getBatchId())
                .build());
    }

    @PostMapping("/send/{candidateId}")
    @Operation(summary = "Send message to a single candidate",
            description = "Sends the resolved template message immediately to a single candidate and records logs")
    public ResponseEntity<SendResponse> sendSingle(
            @PathVariable("candidateId") Long candidateId,
            @RequestParam("templateId") Long templateId) {

        Candidate candidate = candidateRepository.findById(candidateId).orElse(null);
        if (candidate == null) {
            return ResponseEntity.notFound().build();
        }

        MessageTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template == null) {
            return ResponseEntity.badRequest().body(SendResponse.builder()
                    .success(false)
                    .message("Template not found")
                    .build());
        }

        MessageLog logEntry = messageLogRepository.findByCandidateIdAndTemplateId(candidateId, templateId)
                .orElseGet(() -> MessageLog.builder()
                        .batchId(candidate.getBatch().getId())
                        .candidateId(candidateId)
                        .templateId(templateId)
                        .status("PENDING")
                        .build());

        try {
            String messageText = templateService.resolveTemplate(template.getContent(), candidate);
            String messageId = whatsAppService.sendMessage(candidate.getWhatsappNumber(), messageText, template.getName(), candidate);

            logEntry.setStatus("SENT");
            logEntry.setWhatsappMessageId(messageId);
            logEntry.setSentAt(LocalDateTime.now());
            logEntry.setErrorMessage(null);
            messageLogRepository.save(logEntry);

            return ResponseEntity.ok(SendResponse.builder()
                    .success(true)
                    .message("Message sent successfully to " + candidate.getName())
                    .build());
        } catch (Exception e) {
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
            messageLogRepository.save(logEntry);

            return ResponseEntity.internalServerError().body(SendResponse.builder()
                    .success(false)
                    .message("Failed to send message: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/status/{batchId}")
    @Operation(summary = "Check delivery logs for a batch", description = "Returns detailed records of all messages sent or failed for the batch")
    public ResponseEntity<List<MessageLog>> getLogsByBatch(@PathVariable("batchId") Long batchId) {
        if (!batchRepository.existsById(batchId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(messageLogRepository.findByBatchId(batchId));
    }

    @GetMapping("/stats/{batchId}")
    @Operation(summary = "Get send statistics for a batch", description = "Returns counts of PENDING, SENT, FAILED, and DELIVERED messages")
    public ResponseEntity<BatchStatsResponse> getStatsByBatch(@PathVariable("batchId") Long batchId) {
        Batch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            return ResponseEntity.notFound().build();
        }

        long pending = messageLogRepository.countByBatchIdAndStatus(batchId, "PENDING");
        long sent = messageLogRepository.countByBatchIdAndStatus(batchId, "SENT");
        long failed = messageLogRepository.countByBatchIdAndStatus(batchId, "FAILED");
        long delivered = messageLogRepository.countByBatchIdAndStatus(batchId, "DELIVERED");

        // If no message logs exist yet, the candidates are all effectively "PENDING"
        if (pending == 0 && sent == 0 && failed == 0 && delivered == 0) {
            pending = batch.getTotalCandidates();
        }

        BatchStatsResponse stats = BatchStatsResponse.builder()
                .batchId(batchId)
                .fileName(batch.getFileName())
                .status(batch.getStatus())
                .totalCandidates(batch.getTotalCandidates())
                .pendingCount(pending)
                .sentCount(sent)
                .failedCount(failed)
                .deliveredCount(delivered)
                .build();

        return ResponseEntity.ok(stats);
    }
}
