package com.rajkumar.auth.service;

import com.rajkumar.auth.domain.AppUser;
import com.rajkumar.auth.domain.Role;
import com.rajkumar.auth.repository.UserRepository;
import com.rajkumar.auth.web.dto.LoginRequest;
import com.rajkumar.auth.web.dto.RegisterRequest;
import com.rajkumar.auth.web.dto.TokenResponse;
import com.rajkumar.auth.web.dto.UserResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** New accounts always get the USER role; only an admin can promote. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username '" + username + "' is already taken");
        }
        if (users.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        AppUser user = users.save(new AppUser(username, email, passwordEncoder.encode(request.password()), Role.USER));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // Same message whether the user or the password is wrong, so usernames can't be probed.
        AppUser user = users.findByUsername(request.username().trim())
                .filter(AppUser::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        TokenService.IssuedToken token = tokenService.issue(user);
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        return users.findByUsername(username).map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException("User '" + username + "' not found"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> allUsers() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse changeRole(long userId, Role role) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));
        user.changeRole(role);
        return UserResponse.from(user);
    }
}
