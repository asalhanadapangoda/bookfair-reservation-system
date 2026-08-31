package com.bookfair.backend.service.impl;

import com.bookfair.backend.dto.StallRequest;
import com.bookfair.backend.dto.StallResponse;
import com.bookfair.backend.enums.StallSize;
import com.bookfair.backend.enums.StallStatus;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.service.StallService;
import com.bookfair.backend.util.CommonMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StallServiceImpl implements StallService {

    private final StallRepository stallRepository;

    @Override
    public StallResponse createStall(StallRequest stallRequest) {
        verifyAdminAccess();
        if (stallRepository.existsByStallCode(stallRequest.getStallCode())) {
            throw new RuntimeException(CommonMessages.STALL_CODE_ALREADY_EXISTS);
        }

        Stall stall = new Stall();
        stall.setStallCode(stallRequest.getStallCode());
        stall.setStallSize(stallRequest.getStallSize());
        stall.setPrice(stallRequest.getPrice());
        stall.setStallStatus(StallStatus.AVAILABLE);

        Stall savedStall = stallRepository.save(stall);
        return mapToResponse(savedStall);
    }

    @Override
    public StallResponse getStallById(Long stallId) {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException(CommonMessages.STALL_NOT_FOUND));
        return mapToResponse(stall);
    }

    @Override
    public List<StallResponse> getAllStalls() {
        return stallRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StallResponse> getAvailableStalls() {
        return stallRepository.findByStallStatus(StallStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StallResponse> getStallsBySize(StallSize stallSize) {
        return stallRepository.findByStallSize(stallSize)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StallResponse updateStall(Long id, StallRequest stallRequest) {
        verifyAdminAccess();
        Stall stall = stallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CommonMessages.STALL_NOT_FOUND));

        stall.setStallCode(stallRequest.getStallCode());
        stall.setStallSize(stallRequest.getStallSize());
        stall.setPrice(stallRequest.getPrice());
        
        // [ENTERPRISE GOVERNANCE] State Transition Guardrails
        if (stallRequest.getStallStatus() != null && stallRequest.getStallStatus() != stall.getStallStatus()) {
            
            // 1. Check for Active/Pending Reservations
            boolean hasActiveReservation = stall.getReservation() != null && 
                (stall.getReservation().getReservationStatus() == com.bookfair.backend.enums.ReservationStatus.CONFIRMED || 
                 stall.getReservation().getReservationStatus() == com.bookfair.backend.enums.ReservationStatus.PENDING);

            if (hasActiveReservation) {
                // Determine if the transition is allowed
                // Basically, if there's an active reservation, you can't manually set it to AVAILABLE, MAINTENANCE, or BLOCKED
                // It must be released via the reservation flow.
                throw new RuntimeException("Can't change status. Stall has an active or pending reservation.");
            }

            stall.setStallStatus(stallRequest.getStallStatus());
        }

        return mapToResponse(stallRepository.save(stall));
    }

    @Override
    public void deleteStall(Long id) {
        verifyAdminAccess();
        Stall stall = stallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CommonMessages.STALL_NOT_FOUND));

        // [GUARDRAIL] Stall Shield: Block deletion if part of an active confirmed reservation
        if (stall.getReservation() != null && 
            stall.getReservation().getReservationStatus() == com.bookfair.backend.enums.ReservationStatus.CONFIRMED) {
            throw new RuntimeException(CommonMessages.STALL_IN_ACTIVE_RESERVATION);
        }

        stallRepository.delete(stall);
    }

    private StallResponse mapToResponse(Stall stall) {
        return StallResponse.builder()
                .stallId(stall.getId())
                .stallCode(stall.getStallCode())
                .stallSize(stall.getStallSize())
                .price(stall.getPrice())
                .stallStatus(stall.getStallStatus())
                .build();
    }

    private void verifyAdminAccess() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }
    }
}
