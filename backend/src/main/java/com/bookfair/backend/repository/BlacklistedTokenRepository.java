package com.bookfair.backend.repository;
import com.bookfair.backend.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
}
