package com.bookfair.backend.service.impl;

import com.bookfair.backend.dto.ReservationRequest;
import com.bookfair.backend.dto.ReservationResponse;
import com.bookfair.backend.enums.ReservationStatus;
import com.bookfair.backend.enums.StallStatus;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.exception.ValidationException;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.service.ReservationService;
import com.bookfair.backend.service.QrCodeService;
import com.bookfair.backend.util.CommonMessages;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final StallRepository stallRepository;

    private final QrCodeService qrCodeService;

    @Transactional
    @Override
    public ReservationResponse createReservation(ReservationRequest reservationRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("Not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        User user;
        if (isAdmin && reservationRequest.getUserId() != null) {
            user = userRepository.findById(reservationRequest.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.USER_NOT_FOUND));
        } else {
            String currentUserEmail = authentication.getName();
            user = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.USER_NOT_FOUND));
        }

        List<Reservation> userReservations = reservationRepository.findByUserId(user.getId());
        long activeStallsCount = userReservations.stream()
                .filter(r -> r.getReservationStatus() != ReservationStatus.CANCELLED)
                .mapToLong(r -> r.getStalls().size())
                .sum();

        if (activeStallsCount + reservationRequest.getStallIds().size() > 3) {
            throw new ValidationException(CommonMessages.MAX_STALLS_EXCEEDED);
        }

        List<Stall> stalls = stallRepository.findAllById(reservationRequest.getStallIds());

        if (stalls.size() != reservationRequest.getStallIds().size()) {
            throw new ResourceNotFoundException(CommonMessages.STALL_NOT_FOUND);
        }

        if (stalls.size() > 3) {
            throw new ValidationException(CommonMessages.MAX_STALLS_EXCEEDED);
        }

        for (Stall stall : stalls) {
            if (stall.getStallStatus() != StallStatus.AVAILABLE) {
                throw new ValidationException(CommonMessages.STALL_NOT_AVAILABLE);
            }
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setReservationStatus(ReservationStatus.PENDING);

        Reservation savedReservation = reservationRepository.save(reservation);

        double totalAmount = 0;

        for (Stall stall : stalls) {
            stall.setReservation(savedReservation);
            stall.setStallStatus(StallStatus.RESERVED);
            totalAmount += stall.getPrice();
        }

        savedReservation.setTotalAmount(totalAmount);
        savedReservation.setStalls(stalls); // Explicitly set stalls for response mapping
        stallRepository.saveAll(stalls);
        reservationRepository.save(savedReservation);

        return mapToResponse(savedReservation);
    }

    @Override
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ReservationResponse> getReservationByUserId(Long userId) {
        return reservationRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.RESERVATION_NOT_FOUND));
        verifyOwnership(reservation);
        return mapToResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.RESERVATION_NOT_FOUND));
        verifyOwnership(reservation);

        if (reservation.getReservationStatus() != ReservationStatus.PENDING) {
            throw new ValidationException("Only pending reservations can be cancelled.");
        }

        reservation.setReservationStatus(ReservationStatus.CANCELLED);

        for (Stall stall : reservation.getStalls()) {
            stall.setReservation(null);
            stall.setStallStatus(StallStatus.AVAILABLE);
        }
        stallRepository.saveAll(reservation.getStalls());
        return mapToResponse(reservationRepository.save(reservation));
    }

    @Override
    public ReservationResponse updateReservationStatus(Long id, String status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.RESERVATION_NOT_FOUND));
        verifyOwnership(reservation);

        try {
            ReservationStatus newStatus = ReservationStatus.valueOf(status.toUpperCase());
            reservation.setReservationStatus(newStatus);

            if (newStatus == ReservationStatus.CANCELLED) {
                return cancelReservation(id);
            }

        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status");
        }

        return mapToResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest reservationRequest) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.RESERVATION_NOT_FOUND));
        verifyOwnership(reservation);

        if (reservationRequest.getTotalAmount() != null) {
            reservation.setTotalAmount(reservationRequest.getTotalAmount());
        }

        if (reservationRequest.getReservationStatus() != null) {
            reservation.setReservationStatus(reservationRequest.getReservationStatus());

            if (reservationRequest.getReservationStatus() == ReservationStatus.CANCELLED) {
                // Return result of cancelReservation for full cleanup
                return cancelReservation(id);
            }
        }

        return mapToResponse(reservationRepository.save(reservation));
    }

    @Override
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.RESERVATION_NOT_FOUND));
        verifyOwnership(reservation);

        // [GUARDRAIL] Stall Shield: Block deletion of active or confirmed reservations
        if (reservation.getReservationStatus() == ReservationStatus.PENDING || 
            reservation.getReservationStatus() == ReservationStatus.CONFIRMED) {
            throw new ValidationException(CommonMessages.STALL_IN_ACTIVE_RESERVATION);
        }

        for (Stall stall : reservation.getStalls()) {
            stall.setReservation(null);
            stall.setStallStatus(StallStatus.AVAILABLE);
        }
        stallRepository.saveAll(reservation.getStalls());

        reservationRepository.deleteById(id);
    }

    @Override
    public byte[] generateQrCode(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(CommonMessages.RESERVATION_NOT_FOUND));

        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException(CommonMessages.QR_CODE_ONLY_FOR_CONFIRMED);
        }

        String content = "Reservation ID: " + reservation.getId() +
                "\nUser: " + reservation.getUser().getEmail() +
                "\nStalls: "
                + reservation.getStalls().stream().map(Stall::getStallCode).collect(Collectors.joining(", "));

        return qrCodeService.generateQrCode(content, 300, 300);
    }

    private void verifyOwnership(Reservation reservation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("Not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return;
        }

        String currentUserEmail = authentication.getName();
        if (reservation.getUser() != null && !reservation.getUser().getEmail().equals(currentUserEmail)) {
            throw new ValidationException("Unauthorized access to reservation");
        }
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        String businessName = "Unknown";
        String email = "";
        String contactNumber = "";
        Long userId = null;
        List<String> stallCodes = java.util.Collections.emptyList();

        if (reservation.getUser() != null) {
            userId = reservation.getUser().getId();
            businessName = reservation.getUser().getBusinessName();
            email = reservation.getUser().getEmail();
            contactNumber = reservation.getUser().getContactNumber();
        }

        if (reservation.getStalls() != null) {
            stallCodes = reservation.getStalls().stream()
                    .map(Stall::getStallCode)
                    .collect(Collectors.toList());
        }

        return ReservationResponse.builder()
                .reservationId(reservation.getId())
                .userId(userId)
                .stallCodes(stallCodes)
                .totalAmount(reservation.getTotalAmount())
                .reservationDate(reservation.getReservationDate())
                .reservationStatus(reservation.getReservationStatus())
                .businessName(businessName)
                .contactNumber(contactNumber)
                .email(email)
                .build();
    }
}
