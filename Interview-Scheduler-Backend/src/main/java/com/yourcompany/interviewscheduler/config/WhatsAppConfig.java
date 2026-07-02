package com.yourcompany.interviewscheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "whatsapp")
@Data
public class WhatsAppConfig {

    private String provider = "mock";

    private boolean mockMode = true;

    // Meta Cloud API
    private Meta meta = new Meta();

    @Data
    public static class Meta {
        private String baseUrl = "https://graph.facebook.com";
        private String apiVersion = "v23.0";
        private String phoneNumberId;
        private String businessAccountId;
        private String senderNumber;
        private String accessToken;
        private String verifyToken = "tvmFirstSecretVerifyToken";
    }
}