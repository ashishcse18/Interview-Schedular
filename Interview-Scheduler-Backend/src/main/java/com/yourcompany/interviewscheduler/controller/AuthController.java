package com.yourcompany.interviewscheduler.controller;

import com.yourcompany.interviewscheduler.model.entity.User;
import com.yourcompany.interviewscheduler.repository.UserRepository;
import com.yourcompany.interviewscheduler.service.EmailService;
import com.yourcompany.interviewscheduler.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String email = payload.get("email");
        String password = payload.get("password");

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields (name, email, password) are required."));
        }

        if (userRepository.findByEmail(email.trim().toLowerCase()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email address is already registered."));
        }

        User user = User.builder()
                .name(name.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(SecurityUtils.hashPassword(password))
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Registration successful. Please login.",
            "userId", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "profilePicture", ""
        ));
    }

    @PostMapping("/login-password")
    public ResponseEntity<?> loginPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty() || !SecurityUtils.verifyPassword(password, userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "message", "Login successful",
            "userId", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "profilePicture", user.getProfilePicture() != null ? user.getProfilePicture() : ""
        ));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Email is not registered. Please register first."));
        }

        User user = userOpt.get();
        
        // Generate 6 digit random OTP code
        String otp = String.format("%06d", new Random().nextInt(1000000));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        System.out.println("\n[OTP SERVICE] Generated OTP for user " + user.getName() + " (" + user.getEmail() + "): " + otp + "\n");

        // Dispatch email
        emailService.sendOtpEmail(user.getEmail(), otp);

        return ResponseEntity.ok(Map.of(
            "message", "OTP sent successfully. (For testing, OTP is " + otp + ")",
            "otp", otp
        ));
    }

    @PostMapping("/login-otp")
    public ResponseEntity<?> loginOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        if (email == null || email.trim().isEmpty() ||
            otp == null || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email."));
        }

        User user = userOpt.get();
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp.trim())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid OTP code."));
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("message", "OTP has expired. Please request a new one."));
        }

        // Clear OTP on success
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Login successful",
            "userId", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "profilePicture", user.getProfilePicture() != null ? user.getProfilePicture() : ""
        ));
    }

    @PostMapping("/profile-picture")
    public ResponseEntity<?> updateProfilePicture(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String profilePicture = payload.get("profilePicture");

        if (email == null || email.trim().isEmpty() || profilePicture == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and profilePicture parameters are required."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found."));
        }

        User user = userOpt.get();
        user.setProfilePicture(profilePicture);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Profile picture updated successfully.",
            "userId", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "profilePicture", user.getProfilePicture()
        ));
    }
}
