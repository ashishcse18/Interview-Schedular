package com.yourcompany.interviewscheduler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public void sendOtpEmail(String toEmail, String otpCode) {
        if (mailSender == null || smtpHost == null || smtpHost.trim().isEmpty() || smtpUsername == null || smtpUsername.trim().isEmpty() || smtpUsername.contains("your-email")) {
            System.out.println("\n[SIMULATED EMAIL SERVICE] Real-time email sending is not configured (SMTP credentials are default in application.properties).");
            System.out.println("[SIMULATED EMAIL SERVICE] OTP for " + toEmail + " is: " + otpCode + "\n");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(smtpUsername);
            message.setTo(toEmail);
            message.setSubject("Interview Scheduler - OTP Verification");
            message.setText("Hello,\n\nYour one-time password (OTP) for registration/login is: " + otpCode + "\n\nThis OTP will expire in 5 minutes.\n\nBest regards,\nInterview Scheduler Team");

            mailSender.send(message);
            System.out.println("\n[EMAIL SERVICE] Real-time OTP email sent successfully to: " + toEmail + "\n");
        } catch (Exception e) {
            System.err.println("\n[EMAIL SERVICE ERROR] Failed to send real-time email to " + toEmail + ": " + e.getMessage());
            System.out.println("[EMAIL SERVICE FALLBACK] OTP code logged: " + otpCode + "\n");
        }
    }
}
