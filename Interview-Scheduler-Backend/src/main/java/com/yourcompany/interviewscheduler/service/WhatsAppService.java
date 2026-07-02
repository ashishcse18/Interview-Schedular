    package com.yourcompany.interviewscheduler.service;

    import com.yourcompany.interviewscheduler.config.WhatsAppConfig;
    import com.yourcompany.interviewscheduler.exception.WhatsAppSendException;
    import com.yourcompany.interviewscheduler.exception.NonRetryableWhatsAppException;
    import com.yourcompany.interviewscheduler.model.entity.Candidate;
    import jakarta.annotation.PostConstruct;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.*;
    import org.springframework.retry.annotation.Backoff;
    import org.springframework.retry.annotation.Recover;
    import org.springframework.retry.annotation.Retryable;
    import org.springframework.stereotype.Service;
    import org.springframework.web.client.RestTemplate;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.UUID;

    @Service
    @Slf4j
    public class WhatsAppService {

        private final WhatsAppConfig config;
        private final RestTemplate restTemplate;

        public WhatsAppService(WhatsAppConfig config) {
            this.config = config;
            this.restTemplate = new RestTemplate();
        }




        @PostConstruct
        public void init() {
            log.info("Configured WhatsApp Provider: {}", config.getProvider());
            if (config.getMeta() != null) {
                log.info("Phone Number ID: {}", config.getMeta().getPhoneNumberId());
                log.info("Sender Number: {}", config.getMeta().getSenderNumber());
            }
        }

        /**
         * Sends a WhatsApp message (fallback text method).
         */
        public String sendMessage(String toMobile, String messageBody) {
            return sendMessage(toMobile, messageBody, null, null);
        }

        /**
         * Sends a WhatsApp message with dynamic template and candidate details.
         * Retries up to 3 times on standard Exception, doubling the delay starting at 1000ms.
         */
        @Retryable(
                retryFor = {Exception.class},
                noRetryFor = {NonRetryableWhatsAppException.class},
                maxAttempts = 3,
                backoff = @Backoff(delay = 1000, multiplier = 2.0)
        )
        public String sendMessage(String toMobile, String messageBody, String templateName, Candidate candidate) {
            String provider = config.getProvider();

            if ("mock".equalsIgnoreCase(provider)) {
                log.info("[MOCK WHATSAPP] Sending message to {}:\n{}", toMobile, messageBody);
                return "MOCK-SID-" + UUID.randomUUID().toString();
            }

            if ("meta".equalsIgnoreCase(provider)) {
                return sendMetaMessage(toMobile, messageBody, templateName, candidate);
            }

            if ("twilio".equalsIgnoreCase(provider)) {
                log.info("[MOCK TWILIO fallback] Sending message to {}:\n{}", toMobile, messageBody);
                return "MOCK-TWILIO-SID-" + UUID.randomUUID().toString();
            }

            log.warn("Unknown provider: '{}'. Falling back to logging message in MOCK mode.", provider);
            log.info("[MOCK FALLBACK] Sending message to {}:\n{}", toMobile, messageBody);
            return "MOCK-FALLBACK-SID-" + UUID.randomUUID().toString();
        }



        private String sendMetaMessage(String toMobile, String messageBody, String templateName, Candidate candidate) {
            String url = String.format("%s/%s/%s/messages",
                    config.getMeta().getBaseUrl(),
                    config.getMeta().getApiVersion(),
                    config.getMeta().getPhoneNumberId());

            // Format recipient phone number by stripping '+' for Meta Cloud API
            String formattedTo = toMobile.startsWith("+") ? toMobile.substring(1) : toMobile;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getMeta().getAccessToken());

            // Construct request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", formattedTo);

            // Check if we should send as Meta Template
            boolean isTemplate = false;
            String metaTemplateName = "";
            if (templateName != null) {
                String normName = templateName.trim();
                String lowerName = normName.toLowerCase();
                // If template name has no spaces, or is prefixed with meta_/template_, or is hello_world
                if (lowerName.equals("hello_world") ||
                    lowerName.startsWith("meta_") ||
                    lowerName.startsWith("template_") ||
                    (!normName.contains(" ") && normName.length() > 0)) {

                    isTemplate = true;
                    if (lowerName.startsWith("meta_")) {
                        metaTemplateName = normName.substring(5);
                    } else if (lowerName.startsWith("template_")) {
                        metaTemplateName = normName.substring(9);
                    } else {
                        metaTemplateName = normName;
                    }
                }
            }

            if (isTemplate) {
                log.info("Sending Meta Template Message '{}' to {}", metaTemplateName, formattedTo);
                requestBody.put("type", "template");

                Map<String, Object> templateObj = new HashMap<>();
                templateObj.put("name", metaTemplateName);

                Map<String, Object> languageObj = new HashMap<>();
                languageObj.put("code", "en_US");
                templateObj.put("language", languageObj);

                // Add body parameters for custom template (hello_world doesn't require parameters)
                if (!"hello_world".equals(metaTemplateName.toLowerCase()) && candidate != null) {
                    List<Map<String, Object>> components = new ArrayList<>();
                    Map<String, Object> bodyComponent = new HashMap<>();
                    bodyComponent.put("type", "body");

                    List<Map<String, Object>> paramsList = new ArrayList<>();

                    // standard order of parameters: candidateName, companyName, role, panelTiming, gmeetLink, interviewerName
                    String timingStr = candidate.getPanelTiming() != null
                            ? candidate.getPanelTiming().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                            : "";
                    String interviewer = candidate.getInterviewerName() != null && !candidate.getInterviewerName().trim().isEmpty()
                            ? candidate.getInterviewerName() : "HR Team";

                    String[] params = {
                            candidate.getName(),
                            candidate.getCompanyName(),
                            candidate.getRole(),
                            timingStr,
                            candidate.getGmeetLink(),
                            interviewer
                    };

                    for (String param : params) {
                        Map<String, Object> paramMap = new HashMap<>();
                        paramMap.put("type", "text");
                        paramMap.put("text", param != null ? param : "");
                        paramsList.add(paramMap);
                    }

                    bodyComponent.put("parameters", paramsList);
                    components.add(bodyComponent);
                    templateObj.put("components", components);
                }

                requestBody.put("template", templateObj);
            } else {
                log.info("Sending free-form text message to {}", formattedTo);
                requestBody.put("type", "text");
                Map<String, Object> textObj = new HashMap<>();
                textObj.put("preview_url", false);
                textObj.put("body", messageBody);
                requestBody.put("text", textObj);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            try {
                log.debug("Sending real WhatsApp message via Meta Cloud API to {}", formattedTo);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map body = response.getBody();
                    List messagesList = (List) body.get("messages");
                    if (messagesList != null && !messagesList.isEmpty()) {
                        Map msgMap = (Map) messagesList.get(0);
                        String messageId = (String) msgMap.get("id");
                        log.info("Message sent successfully via Meta to {} with SID: {}", formattedTo, messageId);
                        return messageId;
                    }
                    return "SUCCESS";
                } else {
                    throw new WhatsAppSendException("Failed with status code: " + response.getStatusCode());
                }
            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                String errorBody = e.getResponseBodyAsString();
                log.error("Meta API error response: {}", errorBody);
                if (e.getStatusCode().is4xxClientError()) {
                    throw new NonRetryableWhatsAppException("Meta API Client Error: " + e.getMessage() + " - Details: " + errorBody, e);
                }
                throw new WhatsAppSendException("Failed to send WhatsApp message via Meta: " + e.getMessage() + " - Details: " + errorBody, e);
            } catch (Exception e) {
                log.error("Attempt failed to send WhatsApp message via Meta to {}: {}", formattedTo, e.getMessage());
                throw new WhatsAppSendException("Failed to send WhatsApp message via Meta: " + e.getMessage(), e);
            }
        }

        /**
         * Fallback recovery when all retry attempts fail.
         */
        @Recover
        public String recover(Exception e, String toMobile, String messageBody, String templateName, Candidate candidate) {
            log.error("Exhausted all retries for sending WhatsApp message to {}. Final Error: {}", toMobile, e.getMessage());
            throw new WhatsAppSendException("Failed to send WhatsApp message after 3 attempts: " + e.getMessage(), e);
        }
    }
