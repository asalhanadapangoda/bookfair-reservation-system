package com.bookfair.backend.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
}
