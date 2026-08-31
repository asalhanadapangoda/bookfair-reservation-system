package com.bookfair.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final com.bookfair.backend.repository.BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = null;
        String userEmail = null;

        // Extract JWT from the authToken cookie
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("authToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }
        
        // Fallback to Authorization header if no cookie is present (useful for external API clients if needed)
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (blacklistedTokenRepository.existsByToken(jwt)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token has been revoked/logged out");
            return;
        }

        try {
            userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Read claims directly from token instead of hitting the database
                String role = jwtService.extractClaim(jwt, claims -> claims.get("role", String.class));
                Long userId = jwtService.extractClaim(jwt, claims -> claims.get("userId", Long.class));
                String name = jwtService.extractClaim(jwt, claims -> claims.get("name", String.class));
                
                if (role != null && jwtService.isTokenValid(jwt, userEmail)) {
                    // Reconstruct UserDetails from claims
                    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                            userEmail,
                            "", // No password needed for JWT validation
                            java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    // We can also store the extra claims in the details object for easy access in controllers
                    java.util.Map<String, Object> details = new java.util.HashMap<>();
                    details.put("userId", userId);
                    details.put("name", name);
                    details.put("webDetails", new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    authToken.setDetails(details);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Ignore invalid tokens and let the request proceed (it will be blocked if authentication is required)
        }
        
        filterChain.doFilter(request, response);
    }
}
