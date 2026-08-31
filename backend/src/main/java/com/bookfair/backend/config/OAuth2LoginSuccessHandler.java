package com.bookfair.backend.config;

import com.bookfair.backend.enums.Role;
import com.bookfair.backend.model.User;
import com.bookfair.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setContactPerson(name);
            newUser.setBusinessName(name); // Default business name for new OIDC users
            newUser.setRole(Role.BUSINESS);
            // Default password since OIDC users don't have one in our system
            newUser.setPassword(""); 
            return userRepository.save(newUser);
        });

        // Generate our JWT
        String token = jwtService.generateToken(user);

        // Set the token in an HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("authToken", token)
                .httpOnly(true)
                .secure(false) // Set to true in production
                .path("/")
                .maxAge(36000) // 10 hours
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());

        // Redirect back to frontend
        getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/profile");
    }
}
