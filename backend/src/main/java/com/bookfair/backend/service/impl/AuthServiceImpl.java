package com.bookfair.backend.service.impl;

import com.bookfair.backend.config.JwtService;
import com.bookfair.backend.dto.AuthResponse;
import com.bookfair.backend.dto.LoginRequest;
import com.bookfair.backend.dto.RegisterRequest;
import com.bookfair.backend.enums.Role;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.service.AuthService;
import com.bookfair.backend.util.CommonMessages;
import com.bookfair.backend.model.PasswordResetToken;
import com.bookfair.backend.repository.PasswordResetTokenRepository;
import com.bookfair.backend.service.EmailService;
import java.time.LocalDateTime;
import java.util.UUID;
import java.security.SecureRandom;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository, JwtService jwtService,
                           PasswordResetTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        try {
            if (userRepository.existsByEmail(registerRequest.getEmail())){
                throw new RuntimeException(CommonMessages.EMAIL_ALREADY_EXISTS);
            }

            validatePasswordComplexity(registerRequest.getPassword());

            User user = new User();
            user.setBusinessName(registerRequest.getBusinessName());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setContactNumber(registerRequest.getContactNumber());
            user.setAddress(registerRequest.getAddress());
            user.setContactPerson(registerRequest.getContactPerson());
            user.setRole(Role.BUSINESS);

            userRepository.save(user);

            String token = jwtService.generateToken(user);
            return new AuthResponse(user.getId(), user.getBusinessName(), user.getContactPerson(), token);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(()->new RuntimeException(CommonMessages.INVALID_EMAIL_OR_PASSWORD));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new RuntimeException(CommonMessages.INVALID_EMAIL_OR_PASSWORD);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(user.getId(), user.getBusinessName(), user.getContactPerson(), token);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(CommonMessages.USER_NOT_FOUND));

        tokenRepository.deleteByUser(user);

        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        String token = String.valueOf(otp);
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Override
    public void verifyOtp(String email, String otp) {
        PasswordResetToken resetToken = tokenRepository.findByToken(otp)
                .orElseThrow(() -> new RuntimeException("Invalid or missing OTP"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("OTP has expired");
        }

        if (!resetToken.getUser().getEmail().equals(email)) {
            throw new RuntimeException("OTP does not belong to this email");
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void resetPassword(com.bookfair.backend.dto.PasswordResetRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getOtp())
                .orElseThrow(() -> new RuntimeException("Invalid or missing OTP"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token has expired");
        }

        if (!resetToken.getUser().getEmail().equals(request.getEmail())) {
            throw new RuntimeException("Token does not belong to this email");
        }

        User user = resetToken.getUser();
        
        validatePasswordComplexity(request.getNewPassword());
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        tokenRepository.delete(resetToken);
    }

    private void validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new RuntimeException("Password must include uppercase, lowercase, number, and symbol");
        }
    }
}
