package org.example.pcbuilder.authservice.controller;

import org.example.pcbuilder.authservice.dto.AuthResponse;
import org.example.pcbuilder.authservice.dto.ErrorResponse;
import org.example.pcbuilder.authservice.dto.LoginRequest;
import org.example.pcbuilder.authservice.dto.MailTestRequest;
import org.example.pcbuilder.authservice.dto.RegisterRequest;
import org.example.pcbuilder.authservice.dto.TokenRequest;
import org.example.pcbuilder.authservice.dto.TokenResponse;
import org.example.pcbuilder.authservice.dto.TwoFactorChallengeResponse;
import org.example.pcbuilder.authservice.dto.TwoFactorVerifyRequest;
import org.example.pcbuilder.authservice.service.JwtTokenService;
import org.example.pcbuilder.authservice.service.MailService;
import org.example.pcbuilder.authservice.service.TwoFactorService;
import org.example.pcbuilder.authservice.service.UserAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenService jwtTokenService;
    private final UserAuthService userAuthService;
    private final MailService mailService;
    private final TwoFactorService twoFactorService;

    public AuthController(
            JwtTokenService jwtTokenService,
            UserAuthService userAuthService,
            MailService mailService,
            TwoFactorService twoFactorService
    ) {
        this.jwtTokenService = jwtTokenService;
        this.userAuthService = userAuthService;
        this.mailService = mailService;
        this.twoFactorService = twoFactorService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody RegisterRequest request) {
        var createdUser = userAuthService.register(request.username(), request.email(), request.password());
        if (createdUser.isEmpty()) {
            return ResponseEntity.status(409).body(new ErrorResponse("Username or email already exists"));
        }

        String username = createdUser.get().getUsername();
        String email = createdUser.get().getEmail();
        String role = createdUser.get().getRole().name();
        boolean verified = createdUser.get().isVerified();
        String token = jwtTokenService.issueToken(email, role);
        long expiresAt = jwtTokenService.extractExpiryEpochMillis(token);
        return ResponseEntity.ok(new AuthResponse(username, email, role, verified, token, expiresAt));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest request) {
        var authenticatedUser = userAuthService.authenticate(request.email(), request.password());
        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid credentials"));
        }

        if (authenticatedUser.get().isVerified()) {
            TwoFactorChallengeResponse challengeResponse = twoFactorService.createChallenge(authenticatedUser.get());
            return ResponseEntity.status(202).body(challengeResponse);
        }

        String username = authenticatedUser.get().getUsername();
        String email = authenticatedUser.get().getEmail();
        String role = authenticatedUser.get().getRole().name();
        boolean verified = authenticatedUser.get().isVerified();
        String token = jwtTokenService.issueToken(email, role);
        long expiresAt = jwtTokenService.extractExpiryEpochMillis(token);
        return ResponseEntity.ok(new AuthResponse(username, email, role, verified, token, expiresAt));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Missing Bearer token"));
        }

        String token = authorizationHeader.substring(7).trim();
        boolean revoked = jwtTokenService.revokeToken(token);
        if (!revoked) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid or expired token"));
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Validated @RequestBody TokenRequest request) {
        String token = jwtTokenService.issueToken(request.username());
        long expiresAt = jwtTokenService.extractExpiryEpochMillis(token);
        return ResponseEntity.ok(new TokenResponse(token, expiresAt));
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@Validated @RequestBody MailTestRequest request) {
        try {
            mailService.sendTestEmail(request.email());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(502).body(new ErrorResponse(exception.getMessage()));
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verifyTwoFactor(@Validated @RequestBody TwoFactorVerifyRequest request) {
        var verifiedUser = twoFactorService.verifyChallenge(request.challengeId(), request.code());
        if (verifiedUser.isEmpty()) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid or expired verification code"));
        }

        String username = verifiedUser.get().getUsername();
        String email = verifiedUser.get().getEmail();
        String role = verifiedUser.get().getRole().name();
        boolean verified = verifiedUser.get().isVerified();
        String token = jwtTokenService.issueToken(email, role);
        long expiresAt = jwtTokenService.extractExpiryEpochMillis(token);
        return ResponseEntity.ok(new AuthResponse(username, email, role, verified, token, expiresAt));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enableTwoFactor(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        String email = jwtTokenService.extractSubject(token);
        var user = userAuthService.findByEmail(email);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body(new ErrorResponse("User not found"));
        }

        TwoFactorChallengeResponse challengeResponse = twoFactorService.createEnableChallenge(user.get());
        return ResponseEntity.status(202).body(challengeResponse);
    }

    @PostMapping("/2fa/enable/confirm")
    public ResponseEntity<?> confirmEnableTwoFactor(@Validated @RequestBody TwoFactorVerifyRequest request) {
        var enabledUser = twoFactorService.verifyEnableChallenge(request.challengeId(), request.code());
        if (enabledUser.isEmpty()) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid or expired verification code"));
        }

        var updatedUser = userAuthService.setTwoFactorEnabled(enabledUser.get().getEmail(), true);
        if (updatedUser.isEmpty()) {
            return ResponseEntity.status(404).body(new ErrorResponse("User not found"));
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disableTwoFactor(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        String email = jwtTokenService.extractSubject(token);
        var updatedUser = userAuthService.setTwoFactorEnabled(email, false);
        if (updatedUser.isEmpty()) {
            return ResponseEntity.status(404).body(new ErrorResponse("User not found"));
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestParam String token) {
        return jwtTokenService.isTokenValid(token)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(401).build();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Bearer token");
        }

        return authorizationHeader.substring(7).trim();
    }
}
