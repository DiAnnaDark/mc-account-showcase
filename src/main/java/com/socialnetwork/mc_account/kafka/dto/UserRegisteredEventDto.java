package com.socialnetwork.mc_account.kafka.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserRegisteredEventDto {

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
}