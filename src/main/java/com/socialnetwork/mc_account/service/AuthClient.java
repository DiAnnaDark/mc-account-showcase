package com.socialnetwork.mc_account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialnetwork.mc_account.client.AuthFeignClient;
import feign.FeignException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private static final String USER_ID_CLAIM = "userId";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JWT_SEPARATOR_REGEX = "\\.";
    private static final int JWT_PARTS_LIMIT = 3;
    private static final int JWT_PAYLOAD_INDEX = 1;

    private static final String FAILED_AUTHENTICATION_METRIC = "auth.failed.total";

    private static final String INVALID_AUTHORIZATION_HEADER_MESSAGE = "Missing or invalid Authorization header";
    private static final String INVALID_TOKEN_MESSAGE = "Token is invalid";
    private static final String TOKEN_VALIDATION_FAILED_MESSAGE = "Token validation failed";
    private static final String INVALID_JWT_STRUCTURE_MESSAGE = "Invalid JWT structure";
    private static final String MISSING_USER_ID_CLAIM_MESSAGE = "JWT does not contain userId claim";
    private static final String CANNOT_EXTRACT_USER_ID_MESSAGE = "Cannot extract user id from token";

    private final AuthFeignClient authFeignClient;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public void validateTokenOrThrow(String authHeader) {
        validateAndGetToken(authHeader);
    }

    public UUID requireCurrentUserId(String authHeader) {
        String token = validateAndGetToken(authHeader);
        return extractUserIdFromToken(token);
    }

    private String validateAndGetToken(String authHeader) {
        String token = extractToken(authHeader);

        if (token == null) {
            incrementFailedAuthenticationMetric();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_AUTHORIZATION_HEADER_MESSAGE);
        }

        if (!callValidate(authHeader)) {
            incrementFailedAuthenticationMetric();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, INVALID_TOKEN_MESSAGE);
        }

        return token;
    }

    private boolean callValidate(String authHeader) {
        try {
            return Boolean.TRUE.equals(authFeignClient.validateToken(authHeader));
        } catch (FeignException exception) {
            incrementFailedAuthenticationMetric();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, TOKEN_VALIDATION_FAILED_MESSAGE, exception);
        }
    }

    private UUID extractUserIdFromToken(String token) {
        try {
            String payloadJson = decodeJwtPayload(token);
            JsonNode payload = objectMapper.readTree(payloadJson);
            JsonNode userIdNode = payload.get(USER_ID_CLAIM);

            if (userIdNode == null || userIdNode.asText().isBlank()) {
                throw new IllegalArgumentException(MISSING_USER_ID_CLAIM_MESSAGE);
            }

            return UUID.fromString(userIdNode.asText());
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            incrementFailedAuthenticationMetric();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, CANNOT_EXTRACT_USER_ID_MESSAGE, exception);
        }
    }

    private String decodeJwtPayload(String token) {
        String[] parts = token.split(JWT_SEPARATOR_REGEX, JWT_PARTS_LIMIT);

        if (parts.length < JWT_PARTS_LIMIT) {
            throw new IllegalArgumentException(INVALID_JWT_STRUCTURE_MESSAGE);
        }

        byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[JWT_PAYLOAD_INDEX]);
        return new String(decodedPayload, StandardCharsets.UTF_8);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authHeader.substring(BEARER_PREFIX.length());
    }

    private void incrementFailedAuthenticationMetric() {
        meterRegistry.counter(FAILED_AUTHENTICATION_METRIC).increment();
    }
}