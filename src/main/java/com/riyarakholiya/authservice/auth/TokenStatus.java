package com.riyarakholiya.authservice.auth;

/**
 * Lifecycle of one refresh token.
 *
 * <ul>
 *   <li>{@code ACTIVE}: the newest token in its family; can be rotated once.</li>
 *   <li>{@code ROTATED}: already exchanged for a successor. Presenting it again is either
 *       a grace-window retry or a replay.</li>
 *   <li>{@code REVOKED}: the whole family was killed (replay, logout, or admin action).</li>
 * </ul>
 */
public enum TokenStatus {
    ACTIVE,
    ROTATED,
    REVOKED
}
