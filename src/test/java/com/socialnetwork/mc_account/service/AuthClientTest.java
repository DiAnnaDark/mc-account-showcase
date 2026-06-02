package com.socialnetwork.mc_account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialnetwork.mc_account.client.AuthFeignClient;
import feign.FeignException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthClientTest {

    private AuthFeignClient authFeignClient;
    private MeterRegistry meterRegistry;
    private Counter counter;
    private AuthClient authClient;

    @BeforeEach
    void setUp() {
        authFeignClient = mock(AuthFeignClient.class);
        meterRegistry = mock(MeterRegistry.class);
        counter = mock(Counter.class);

        when(meterRegistry.counter("auth.failed.total")).thenReturn(counter);

        authClient = new AuthClient(
                authFeignClient,
                new ObjectMapper(),
                meterRegistry
        );
    }

    @Test
    void validateTokenOrThrowShouldPassWhenTokenIsValid() {
        String authHeader = "Bearer " + createJwt(UUID.randomUUID());
        when(authFeignClient.validateToken(authHeader)).thenReturn(true);

        assertDoesNotThrow(() -> authClient.validateTokenOrThrow(authHeader));

        verify(authFeignClient).validateToken(authHeader);
        verify(counter, never()).increment();
    }

    @Test
    void validateTokenOrThrowShouldThrowUnauthorizedWhenHeaderIsNull() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.validateTokenOrThrow(null)
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(counter).increment();
        verifyNoInteractions(authFeignClient);
    }

    @Test
    void validateTokenOrThrowShouldThrowUnauthorizedWhenHeaderHasNoBearerPrefix() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.validateTokenOrThrow("invalid-token")
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(counter).increment();
        verifyNoInteractions(authFeignClient);
    }

    @Test
    void validateTokenOrThrowShouldThrowForbiddenWhenAuthServiceReturnsFalse() {
        String authHeader = "Bearer " + createJwt(UUID.randomUUID());
        when(authFeignClient.validateToken(authHeader)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.validateTokenOrThrow(authHeader)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(counter).increment();
    }

    @Test
    void validateTokenOrThrowShouldThrowForbiddenWhenFeignExceptionOccurs() {
        String authHeader = "Bearer " + createJwt(UUID.randomUUID());
        when(authFeignClient.validateToken(authHeader))
                .thenThrow(mock(FeignException.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.validateTokenOrThrow(authHeader)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(counter).increment();
    }

    @Test
    void requireCurrentUserIdShouldReturnUserIdWhenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        String authHeader = "Bearer " + createJwt(userId);
        when(authFeignClient.validateToken(authHeader)).thenReturn(true);

        UUID actual = authClient.requireCurrentUserId(authHeader);

        assertEquals(userId, actual);
        verify(counter, never()).increment();
    }

    @Test
    void requireCurrentUserIdShouldThrowForbiddenWhenJwtHasInvalidStructure() {
        String authHeader = "Bearer invalid-token";
        when(authFeignClient.validateToken(authHeader)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.requireCurrentUserId(authHeader)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(counter).increment();
    }

    @Test
    void requireCurrentUserIdShouldThrowForbiddenWhenJwtDoesNotContainUserId() {
        String authHeader = "Bearer " + createJwtWithoutUserId();
        when(authFeignClient.validateToken(authHeader)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.requireCurrentUserId(authHeader)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(counter).increment();
    }

    @Test
    void requireCurrentUserIdShouldThrowForbiddenWhenUserIdIsBlank() {
        String authHeader = "Bearer " + createJwtWithUserId("");
        when(authFeignClient.validateToken(authHeader)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.requireCurrentUserId(authHeader)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(counter).increment();
    }

    @Test
    void requireCurrentUserIdShouldThrowForbiddenWhenUserIdIsNotUuid() {
        String authHeader = "Bearer " + createJwtWithUserId("not-uuid");
        when(authFeignClient.validateToken(authHeader)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authClient.requireCurrentUserId(authHeader)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(counter).increment();
    }

    private static String createJwt(UUID userId) {
        return createJwtWithPayload("{\"userId\":\"" + userId + "\"}");
    }

    private static String createJwtWithUserId(String userId) {
        return createJwtWithPayload("{\"userId\":\"" + userId + "\"}");
    }

    private static String createJwtWithoutUserId() {
        return createJwtWithPayload("{\"sub\":\"test\"}");
    }

    private static String createJwtWithPayload(String payload) {
        String header = encode("{}");
        String body = encode(payload);
        String signature = encode("signature");
        return header + "." + body + "." + signature;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}