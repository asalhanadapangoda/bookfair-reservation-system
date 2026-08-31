package com.bookfair.backend.controller;

import com.bookfair.backend.dto.AuthResponse;
import com.bookfair.backend.dto.LoginRequest;
import com.bookfair.backend.dto.RegisterRequest;
import com.bookfair.backend.service.AuthService;
import com.bookfair.backend.util.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth controller reachable");
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping({"/verify-email", "/verify_email"})
    public ResponseEntity<?> verifyEmail(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            System.out.println("Verifying email: " + email);
            boolean exists = authService.existsByEmail(email);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error verifying email: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody com.bookfair.backend.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Void> verifyOtp(@RequestBody com.bookfair.backend.dto.VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody com.bookfair.backend.dto.PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
