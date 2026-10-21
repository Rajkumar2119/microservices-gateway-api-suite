package com.rajkumar.auth.web.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
