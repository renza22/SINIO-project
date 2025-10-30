package com.sinio.demo.config;

import com.sinio.demo.model.User;
import com.sinio.demo.model.UserRole;
import com.sinio.demo.repository.UserRepository;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureAdminUser();
        ensureOtherUsersHaveValidRoles();
    }

    private void ensureAdminUser() {
        String normalizedEmail = normalizeEmail(ADMIN_EMAIL);

        userRepository
            .findByEmail(normalizedEmail)
            .map(user -> updateAdminUser(user, DEFAULT_ADMIN_PASSWORD))
            .orElseGet(() -> createAdminUser(normalizedEmail, DEFAULT_ADMIN_PASSWORD));
    }

    private User updateAdminUser(User user, String defaultPassword) {
        boolean updated = false;
        if (user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            updated = true;
        }
        if (!passwordEncoder.matches(defaultPassword, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(defaultPassword));
            updated = true;
        }
        if (updated) {
            return userRepository.save(user);
        }
        return user;
    }

    private User createAdminUser(String email, String defaultPassword) {
        User admin = new User();
        admin.setFullName("Administrator");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
        admin.setRole(UserRole.ADMIN);
        return userRepository.save(admin);
    }

    private void ensureOtherUsersHaveValidRoles() {
        userRepository
            .findAll()
            .forEach(user -> {
                String userEmail = normalizeEmail(user.getEmail());
                boolean isAdminEmail = ADMIN_EMAIL.equalsIgnoreCase(userEmail);
                UserRole role = user.getRole();
                if (role == null || (role == UserRole.ADMIN && !isAdminEmail)) {
                    user.setRole(UserRole.TAMU);
                    userRepository.save(user);
                }
            });
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
