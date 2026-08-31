package com.bookfair.backend.service;

import com.bookfair.backend.dto.AuthResponse;
import com.bookfair.backend.dto.LoginRequest;
import com.bookfair.backend.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register (RegisterRequest registerRequest);
    AuthResponse login (LoginRequest loginRequest);
    boolean existsByEmail(String email);
    void forgotPassword(String email);
    void verifyOtp(String email, String otp);
    void resetPassword(com.bookfair.backend.dto.PasswordResetRequest request);
}
