package com.riyarakholiya.authservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        // BCrypt only hashes the first 72 bytes and rejects longer input, so the cap is enforced here.
        @NotBlank @Size(min = 12, max = 72) String password) {
}
