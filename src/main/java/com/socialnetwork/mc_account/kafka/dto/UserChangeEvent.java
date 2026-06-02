package com.socialnetwork.mc_account.kafka.dto;

import java.util.UUID;

public record UserChangeEvent(
        UUID userId,
        String email
) {
}