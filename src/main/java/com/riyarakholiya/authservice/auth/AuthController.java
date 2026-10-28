package com.riyarakholiya.authservice.auth;

import com.riyarakholiya.authservice.auth.dto.LoginRequest;
import com.riyarakholiya.authservice.auth.dto.RegisterRequest;
import com.riyarakholiya.authservice.auth.dto.TokenResponse;
import com.riyarakholiya.authservice.auth.dto.UserResponse;
import com.riyarakholiya.authservice.config.AuthProperties;
import com.riyarakholiya.authservice.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/auth";

    private final AuthService auth;
    private final RateLimiter refreshPerIp;
    private final Duration sessionTtl;

    AuthController(AuthService auth, @Qualifier("refreshPerIpLimiter") RateLimiter refreshPerIp,
                   AuthProperties properties) {
        this.auth = auth;
        this.refreshPerIp = refreshPerIp;
        this.sessionTtl = properties.refresh().sessionTtl();
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(auth.login(request));
    }

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest request) {
        // The per-IP limit runs before any token lookup, so guessing tokens from one address is limited too.
        refreshPerIp.acquire(request.getRemoteAddr());
        return tokenResponse(auth.refresh(refreshToken));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        auth.logout(refreshToken);
        // Max-Age=0 tells the browser to delete the cookie now.
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<TokenResponse> tokenResponse(AuthTokens tokens) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (tokens.refreshToken() != null) {
            response.header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), sessionTtl).toString());
        }
        return response.body(new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresIn()));
    }

    private static ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
