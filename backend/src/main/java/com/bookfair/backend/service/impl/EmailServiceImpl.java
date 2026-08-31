package com.bookfair.backend.service.impl;

import com.bookfair.backend.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("asalhimsanda@gmail.com");
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("You have requested to reset your password.\n\n" +
                "Please use the following 6-digit OTP to reset your password:\n\n" +
                token + "\n\n" +
                "This OTP will expire in 15 minutes.\n\n" +
                "If you did not request a password reset, please ignore this email.");
        mailSender.send(message);
    }
}
