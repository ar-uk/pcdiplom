package org.example.pcbuilder.communityservice.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community")
public class CommunityController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping(
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        return ResponseEntity.ok(Map.of(
                "service", "community-service",
                "status", "ok",
                "user", authenticatedUser == null ? "anonymous" : authenticatedUser
        ));
    }
}
