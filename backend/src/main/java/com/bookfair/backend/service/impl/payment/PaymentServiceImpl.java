package com.bookfair.backend.service.impl.payment;

import com.bookfair.backend.dto.PaymentRequest;
import com.bookfair.backend.dto.PaymentResponse;
import com.bookfair.backend.enums.PaymentMethod;
import com.bookfair.backend.enums.PaymentStatus;
import com.bookfair.backend.enums.ReservationStatus;
import com.bookfair.backend.model.Payment;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.PaymentRepository;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.service.payment.PaymentService;
import com.bookfair.backend.service.payment.strategy.PaymentProvider;
import com.bookfair.backend.util.CommonMessages;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final List<PaymentProvider> paymentProviders;
    private final com.bookfair.backend.service.QrCodeService qrCodeService;

    @Override
    public Object processPayment(PaymentRequest paymentRequest) {
        Reservation reservation = reservationRepository.findById(paymentRequest.getReservationId())
                .orElseThrow(() -> new RuntimeException(CommonMessages.RESERVATION_NOT_FOUND));

        if (paymentRepository.findByReservationId(reservation.getId()).isPresent()) {
            throw new RuntimeException(CommonMessages.PAYMENT_ALREADY_EXISTS);
        }

        // Cash payments
        if (paymentRequest.getPaymentMethod() == PaymentMethod.CASH){
            Payment payment = Payment.builder()
                    .reservation(reservation)
                    .amount(reservation.getTotalAmount())
                    .paymentMethod(paymentRequest.getPaymentMethod())
                    .paymentDate(LocalDateTime.now())
                    .paymentStatus(PaymentStatus.PENDING) // Default to pending for cash
                    .build();

            reservation.setReservationStatus(ReservationStatus.PENDING);

            reservationRepository.save(reservation);
            paymentRepository.save(payment);

            return mapToPaymentResponse(payment);

        }

        // For other methods, delegate to the proper PaymentProvider
        PaymentProvider provider = getProvider(paymentRequest.getPaymentMethod());

        return provider.initiatePayment(reservation.getId());

    }

    // Cash payment confirmation
    @Override
    public PaymentResponse confirmCashPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException(CommonMessages.PAYMENT_NOT_FOUND));

        if (payment.getPaymentMethod() != PaymentMethod.CASH) {
            throw new RuntimeException(CommonMessages.PAYMENT_CONFIRMATION_REQUIRED);
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.getReservation().setReservationStatus(ReservationStatus.CONFIRMED);
        
        // Generate QR and Email
        qrCodeService.processPaymentSuccess(payment.getReservation());

        return mapToPaymentResponse(payment);
    }

    // Confirmation for paying by other methods
    @Override
    public PaymentResponse confirmPayment(String referenceId, PaymentMethod method, Long reservationId) {

        if (method == PaymentMethod.CASH) {
            throw new RuntimeException(CommonMessages.PAYMENT_CONFIRMATION_REQUIRED);
        }

        // Find the correct provider and delegate
        PaymentProvider provider = getProvider(method);

        PaymentResponse response = provider.confirmPayment(referenceId, reservationId);
        
        if (response.getPaymentStatus() == PaymentStatus.SUCCESS) {
             Reservation reservation = reservationRepository.findById(reservationId)
                     .orElseThrow(() -> new RuntimeException(CommonMessages.RESERVATION_NOT_FOUND));
             qrCodeService.processPaymentSuccess(reservation);
        }

        return response;
    }

    // Getters
    @Override
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException(CommonMessages.PAYMENT_NOT_FOUND));
        verifyOwnership(payment);

        return mapToPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        verifyAdminAccess();
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }

    @Override
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        verifyUserAccess(userId);
        System.out.println("DEBUG: Fetching payments for userId: " + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(CommonMessages.USER_NOT_FOUND));

        List<Payment> payments = paymentRepository.findByReservationUserId(user.getId());
        System.out.println("DEBUG: Found " + payments.size() + " payments for user " + user.getId());
        
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .toList();
    }

    @Override
    public PaymentResponse updatePayment(Long id, PaymentRequest paymentRequest) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CommonMessages.PAYMENT_NOT_FOUND));
        verifyOwnership(payment);

        if (paymentRequest.getAmount() != null) {
            payment.setAmount(paymentRequest.getAmount());
        }
        if (paymentRequest.getPaymentStatus() != null) {
            payment.setPaymentStatus(paymentRequest.getPaymentStatus());
        }
        if (paymentRequest.getPaymentMethod() != null) {
            payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        }
        if (paymentRequest.getTransactionId() != null) {
            payment.setTransactionId(paymentRequest.getTransactionId());
        }

        return mapToPaymentResponse(paymentRepository.save(payment));
    }

    @Override
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CommonMessages.PAYMENT_NOT_FOUND));
        verifyOwnership(payment);

        // [GUARDRAIL] Audit Shield: Block deletion of successful or pending payments
        if (payment.getPaymentStatus() == com.bookfair.backend.enums.PaymentStatus.SUCCESS) {
            throw new RuntimeException("Can't delete successful payments.");
        }
        if (payment.getPaymentStatus() == com.bookfair.backend.enums.PaymentStatus.PENDING) {
            throw new RuntimeException("Can't delete pending payments.");
        }

        paymentRepository.delete(payment);
    }

    // Helper methods
    private PaymentProvider getProvider(PaymentMethod method) {
        return paymentProviders.stream()
                .filter(provider -> provider.getSupportedMethod() == method)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(CommonMessages.PAYMENT_PROVIDER_NOT_FOUND));
    }
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        Reservation reservation = payment.getReservation();
        String businessName = "Unknown";
        String email = "";
        Long userId = null;
        Long reservationId = null;
        List<String> stallCodes = java.util.Collections.emptyList();

        if (reservation != null) {
            reservationId = reservation.getId();
            User user = reservation.getUser();
            if (user != null) {
                userId = user.getId();
                businessName = user.getBusinessName();
                email = user.getEmail();
            }
            if (reservation.getStalls() != null) {
                stallCodes = reservation.getStalls().stream().map(com.bookfair.backend.model.Stall::getStallCode).toList();
            }
        }

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .paymentDate(payment.getPaymentDate())
                .reservationId(reservationId)
                .userId(userId)
                .businessName(businessName)
                .email(email)
                .stallCodes(stallCodes)
                .build();
    }

    private void verifyOwnership(Payment payment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return;
        }

        String currentUserEmail = authentication.getName();
        if (payment.getReservation() != null && 
            payment.getReservation().getUser() != null && 
            !payment.getReservation().getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Unauthorized access to payment");
        }
    }

    private void verifyAdminAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }
    }

    private void verifyUserAccess(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(CommonMessages.USER_NOT_FOUND));

        String currentUserEmail = authentication.getName();
        if (!user.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Unauthorized access to user payments");
        }
    }
}
