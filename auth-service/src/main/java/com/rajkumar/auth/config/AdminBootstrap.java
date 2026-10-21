package com.rajkumar.auth.config;

import com.rajkumar.auth.domain.AppUser;
import com.rajkumar.auth.domain.Role;
import com.rajkumar.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN account from environment variables, if one is configured and missing. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${bootstrap.admin.username:}") String username,
                          @Value("${bootstrap.admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || users.existsByUsername(username)) {
            return;
        }
        users.save(new AppUser(username, username + "@example.com", passwordEncoder.encode(password), Role.ADMIN));
        log.info("Created bootstrap admin user '{}'", username);
    }
}
