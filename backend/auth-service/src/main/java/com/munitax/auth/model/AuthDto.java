package com.munitax.auth.model;

public class AuthDto {
    public record AuthRequest(String username, String password) {}
    public record AuthResponse(String token) {}
}
