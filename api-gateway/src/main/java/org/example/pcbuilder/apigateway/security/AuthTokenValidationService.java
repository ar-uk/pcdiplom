package org.example.pcbuilder.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AuthTokenValidationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String authValidateUrl;

    public AuthTokenValidationService(@Value("${auth.validate-url}") String authValidateUrl) {
        this.authValidateUrl = authValidateUrl;
    }

    public boolean isTokenActive(String token) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(authValidateUrl)
                    .queryParam("token", token)
                    .build(true)
                    .toUriString();

            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            return false;
        }
    }
}