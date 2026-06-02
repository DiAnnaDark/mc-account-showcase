package com.socialnetwork.mc_account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account")
public class Account {

    private static final int ABOUT_MAX_LENGTH = 1000;

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String photo;

    @Column(length = ABOUT_MAX_LENGTH)
    private String about;

    private String city;

    private String country;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "reg_date", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "last_online_time", nullable = false)
    private LocalDateTime lastOnlineTime;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "photo_name")
    private String photoName;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "emoji_status")
    private String emojiStatus;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (regDate == null) {
            regDate = now;
        }
        if (createdOn == null) {
            createdOn = now;
        }
        if (lastOnlineTime == null) {
            lastOnlineTime = now;
        }
        if (isOnline == null) {
            isOnline = false;
        }
        if (isBlocked == null) {
            isBlocked = false;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedOn = LocalDateTime.now();
    }
}