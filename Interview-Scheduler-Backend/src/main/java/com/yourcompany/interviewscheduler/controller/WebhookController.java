package com.yourcompany.interviewscheduler.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.interviewscheduler.config.WhatsAppConfig;
import com.yourcompany.interviewscheduler.model.entity.MessageLog;
import com.yourcompany.interviewscheduler.repository.MessageLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
@CrossOrigin(origins = "*")
@Slf4j
public class WebhookController {

    @Autowired
    private WhatsAppConfig whatsAppConfig;

    @Autowired
    private MessageLogRepository messageLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();


     // Webhook verification endpoint (GET) for Meta Cloud API.
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        log.info("Received Meta Webhook verification request: mode={}, token={}", mode, verifyToken);

        String expectedToken = whatsAppConfig.getMeta() != null ? whatsAppConfig.getMeta().getVerifyToken() : null;
        if (expectedToken == null) {
            expectedToken = "tvmFirstSecretVerifyToken";
        }

        if ("subscribe".equals(mode) && expectedToken.equals(verifyToken)) {
            log.info("Webhook verification successful!");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Webhook verification failed. Mode: {}, Token: {} (Expected: {})", mode, verifyToken, expectedToken);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }


     //Webhook event receiver endpoint (POST) for Meta Cloud API status updates.
    @PostMapping
    public ResponseEntity<Void> receiveWebhookEvent(@RequestBody String payload) {
        log.info("Received Webhook Payload: {}", payload);

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode entryNode = root.path("entry");
            if (entryNode.isArray()) {
                for (JsonNode entry : entryNode) {
                    JsonNode changesNode = entry.path("changes");
                    if (changesNode.isArray()) {
                        for (JsonNode change : changesNode) {
                            JsonNode valueNode = change.path("value");
                            JsonNode statusesNode = valueNode.path("statuses");
                            if (statusesNode.isArray()) {
                                for (JsonNode statusObj : statusesNode) {
                                    String messageId = statusObj.path("id").asText();
                                    String status = statusObj.path("status").asText();
                                    log.info("Found message status update: messageId={}, status={}", messageId, status);

                                    if (messageId != null && !messageId.trim().isEmpty()) {
                                        updateMessageStatus(messageId, status, statusObj);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing webhook payload: {}", e.getMessage(), e);
        }

        // Always return 200 OK to Meta to avoid retries
        return ResponseEntity.ok().build();
    }

    private void updateMessageStatus(String messageId, String status, JsonNode statusObj) {
        Optional<MessageLog> logOpt = messageLogRepository.findByWhatsappMessageId(messageId);
        if (logOpt.isPresent()) {
            MessageLog messageLog = logOpt.get();
            String normalizedStatus = status.toUpperCase();

            // Set the new status
            messageLog.setStatus(normalizedStatus);

            // Handle delivered / read timestamps
            if ("DELIVERED".equals(normalizedStatus) || "READ".equals(normalizedStatus)) {
                messageLog.setDeliveredAt(LocalDateTime.now());
            }

            // Capture details if failed
            if ("FAILED".equals(normalizedStatus)) {
                JsonNode errorsNode = statusObj.path("errors");
                if (errorsNode.isArray() && errorsNode.size() > 0) {
                    JsonNode error = errorsNode.get(0);
                    String errorMsg = String.format("Error %d: %s (Details: %s)",
                            error.path("code").asInt(),
                            error.path("message").asText(),
                            error.path("error_data").path("details").asText());
                    messageLog.setErrorMessage(errorMsg);
                    log.warn("WhatsApp message delivery failed. Error details: {}", errorMsg);
                } else {
                    messageLog.setErrorMessage("Delivery failed (No error details provided by Meta API)");
                }
            } else {
                // Clear error message on success status updates
                messageLog.setErrorMessage(null);
            }

            messageLogRepository.save(messageLog);
            log.info("Updated MessageLog ID {} status to {}", messageLog.getId(), normalizedStatus);
        } else {
            log.warn("No MessageLog record found for Meta message ID: {}", messageId);
        }
    }
}
