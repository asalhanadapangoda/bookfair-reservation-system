package com.bookfair.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(com.bookfair.backend.model.User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuer("http://localhost:8080/api/auth")
                .setAudience("bookfair-client-app")
                .claim("email", user.getEmail())
                .claim("name", user.getContactPerson() != null ? user.getContactPerson() : user.getBusinessName())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, String userEmail) {
        final String username = extractUsername(token);
        final String issuer = extractClaim(token, Claims::getIssuer);
        final String audience = extractClaim(token, Claims::getAudience);
        
        boolean isIssuerValid = "http://localhost:8080/api/auth".equals(issuer);
        boolean isAudienceValid = "bookfair-client-app".equals(audience);
        
        return (username.equals(userEmail)) 
                && !isTokenExpired(token) 
                && isIssuerValid 
                && isAudienceValid;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
