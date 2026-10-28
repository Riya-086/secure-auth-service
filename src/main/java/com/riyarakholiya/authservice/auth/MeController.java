package com.riyarakholiya.authservice.auth;

import com.riyarakholiya.authservice.auth.dto.UserResponse;
import com.riyarakholiya.authservice.user.UserRepository;
import com.riyarakholiya.authservice.web.AuthException;
import com.riyarakholiya.authservice.web.ErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

    private final UserRepository users;

    MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(jwt.getSubject())
                .map(user -> new UserResponse(user.id(), user.email()))
                // A valid token for a deleted user is still unauthorized.
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED));
    }
}
