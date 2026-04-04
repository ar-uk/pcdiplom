package org.example.pcbuilder.authservice.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.example.pcbuilder.authservice.dto.TwoFactorChallengeResponse;
import org.example.pcbuilder.authservice.model.TwoFactorChallenge;
import org.example.pcbuilder.authservice.model.TwoFactorChallengePurpose;
import org.example.pcbuilder.authservice.model.UserAccount;
import org.example.pcbuilder.authservice.repository.TwoFactorChallengeRepository;
import org.example.pcbuilder.authservice.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TwoFactorService {

    private final TwoFactorChallengeRepository challengeRepository;
    private final UserAccountRepository userAccountRepository;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final int challengeTtlMinutes;
    private final int maxAttempts;

    public TwoFactorService(
            TwoFactorChallengeRepository challengeRepository,
            UserAccountRepository userAccountRepository,
            MailService mailService,
            @Value("${auth.two-factor.ttl-minutes:10}") int challengeTtlMinutes,
            @Value("${auth.two-factor.max-attempts:5}") int maxAttempts
    ) {
        this.challengeRepository = challengeRepository;
        this.userAccountRepository = userAccountRepository;
        this.mailService = mailService;
        this.challengeTtlMinutes = challengeTtlMinutes;
        this.maxAttempts = maxAttempts;
    }

    public TwoFactorChallengeResponse createChallenge(UserAccount userAccount) {
        return createChallenge(userAccount, TwoFactorChallengePurpose.LOGIN, "Verification code sent to your email");
    }

    public TwoFactorChallengeResponse createEnableChallenge(UserAccount userAccount) {
        return createChallenge(userAccount, TwoFactorChallengePurpose.ENABLE, "Verification code sent to your email. Use it to turn on 2FA.");
    }

    private TwoFactorChallengeResponse createChallenge(UserAccount userAccount, TwoFactorChallengePurpose purpose, String message) {
        String code = generateCode();
        Instant expiresAt = Instant.now().plusSeconds(challengeTtlMinutes * 60L);

        TwoFactorChallenge challenge = new TwoFactorChallenge();
        challenge.setChallengeId(UUID.randomUUID().toString());
        challenge.setEmail(userAccount.getEmail());
        challenge.setPurpose(purpose);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(expiresAt);
        challenge.setAttempts(0);
        challenge.setUsed(false);

        challengeRepository.save(challenge);
        mailService.sendTwoFactorCode(userAccount.getEmail(), code);

        return new TwoFactorChallengeResponse(
                challenge.getChallengeId(),
                userAccount.getEmail(),
                expiresAt.toEpochMilli(),
            message
        );
    }

    @Transactional
    public Optional<UserAccount> verifyChallenge(String challengeId, String code) {
        Optional<TwoFactorChallenge> challengeOptional = challengeRepository.findByChallengeId(challengeId);
        if (challengeOptional.isEmpty()) {
            return Optional.empty();
        }

        TwoFactorChallenge challenge = challengeOptional.get();
        if (challenge.getPurpose() != TwoFactorChallengePurpose.LOGIN) {
            return Optional.empty();
        }
        if (challenge.isUsed() || challenge.getExpiresAt().isBefore(Instant.now()) || challenge.getAttempts() >= maxAttempts) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        boolean matches = passwordEncoder.matches(code, challenge.getCodeHash());
        if (!matches) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= maxAttempts) {
                challenge.setUsed(true);
            }
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        challenge.setUsed(true);
        challengeRepository.save(challenge);
        return userAccountRepository.findByEmail(challenge.getEmail());
    }

    @Transactional
    public Optional<UserAccount> verifyEnableChallenge(String challengeId, String code) {
        Optional<TwoFactorChallenge> challengeOptional = challengeRepository.findByChallengeId(challengeId);
        if (challengeOptional.isEmpty()) {
            return Optional.empty();
        }

        TwoFactorChallenge challenge = challengeOptional.get();
        if (challenge.getPurpose() != TwoFactorChallengePurpose.ENABLE) {
            return Optional.empty();
        }
        if (challenge.isUsed() || challenge.getExpiresAt().isBefore(Instant.now()) || challenge.getAttempts() >= maxAttempts) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        boolean matches = passwordEncoder.matches(code, challenge.getCodeHash());
        if (!matches) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= maxAttempts) {
                challenge.setUsed(true);
            }
            challengeRepository.save(challenge);
            return Optional.empty();
        }

        challenge.setUsed(true);
        challengeRepository.save(challenge);
        return userAccountRepository.findByEmail(challenge.getEmail());
    }

    private String generateCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}