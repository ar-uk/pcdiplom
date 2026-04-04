package org.example.pcbuilder.authservice.service;

import java.util.Optional;
import org.example.pcbuilder.authservice.model.Role;
import org.example.pcbuilder.authservice.model.UserAccount;
import org.example.pcbuilder.authservice.repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAuthService {

    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserAuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public Optional<UserAccount> register(String username, String email, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);

        if (userAccountRepository.existsByUsername(normalizedUsername) || userAccountRepository.existsByEmail(normalizedEmail)) {
            return Optional.empty();
        }

        UserAccount user = new UserAccount();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setRole(Role.USER);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return Optional.of(userAccountRepository.save(user));
    }

    public Optional<UserAccount> authenticate(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        return userAccountRepository.findByEmail(normalizedEmail)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    public Optional<UserAccount> findByEmail(String email) {
        return userAccountRepository.findByEmail(normalizeEmail(email));
    }

    public Optional<UserAccount> setTwoFactorEnabled(String email, boolean enabled) {
        return userAccountRepository.findByEmail(normalizeEmail(email))
                .map(user -> {
                    user.setVerified(enabled);
                    return userAccountRepository.save(user);
                });
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
