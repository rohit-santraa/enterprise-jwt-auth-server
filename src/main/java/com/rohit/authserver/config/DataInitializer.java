package com.rohit.authserver.config;

import com.rohit.authserver.entity.ERole;
import com.rohit.authserver.entity.Role;
import com.rohit.authserver.entity.User;
import com.rohit.authserver.repository.RoleRepository;
import com.rohit.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Seeds the database with the canonical roles and a default admin + user account on startup.
 * Idempotent: safe to run on every boot — existing rows are left untouched.
 */
@Profile("!prod")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@authserver.com";
    private static final String USER_EMAIL = "user@authserver.com";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdmin();
        seedUser();
    }

    private void seedRoles() {
        for (ERole roleName : ERole.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        log.info("Seeding role: {}", roleName);
                        return roleRepository.save(new Role(roleName));
                    });
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN missing after seeding"));
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing after seeding"));

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode("Admin@12345"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(adminRole, userRole));
        userRepository.save(admin);
        log.info("Seeded default admin account: {} (password: Admin@12345)", ADMIN_EMAIL);
    }

    private void seedUser() {
        if (userRepository.existsByEmail(USER_EMAIL)) {
            return;
        }
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing after seeding"));

        User user = new User();
        user.setUsername("user");
        user.setEmail(USER_EMAIL);
        user.setPassword(passwordEncoder.encode("User@12345"));
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));
        userRepository.save(user);
        log.info("Seeded default user account: {} (password: User@12345)", USER_EMAIL);
    }
}
