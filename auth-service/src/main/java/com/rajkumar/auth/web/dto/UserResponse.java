package com.rajkumar.auth.web.dto;

import com.rajkumar.auth.domain.AppUser;
import com.rajkumar.auth.domain.Role;
import java.time.Instant;

/** Public view of a user. Never includes the password hash. */
public record UserResponse(Long id, String username, String email, Role role, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getCreatedAt());
    }
}
