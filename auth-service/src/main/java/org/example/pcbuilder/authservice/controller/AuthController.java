package org.example.pcbuilder.authservice.controller;

import org.example.pcbuilder.authservice.dto.TokenRequest;
import org.example.pcbuilder.authservice.dto.TokenResponse;
import org.example.pcbuilder.authservice.service.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenService jwtTokenService;

    public AuthController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Validated @RequestBody TokenRequest request) {
        String token = jwtTokenService.issueToken(request.username());
        long expiresAt = jwtTokenService.extractExpiryEpochMillis(token);
        return ResponseEntity.ok(new TokenResponse(token, expiresAt));
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestParam String token) {
        return jwtTokenService.isTokenValid(token)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(401).build();
    }
}
