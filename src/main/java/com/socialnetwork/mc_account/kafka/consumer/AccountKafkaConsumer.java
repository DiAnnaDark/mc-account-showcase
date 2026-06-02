package com.socialnetwork.mc_account.kafka.consumer;

import com.socialnetwork.mc_account.kafka.dto.UserChangeEvent;
import com.socialnetwork.mc_account.kafka.dto.UserRegisteredEventDto;
import com.socialnetwork.mc_account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountKafkaConsumer {

    private static final String USER_REGISTERED_TOPIC = "user-registered";
    private static final String USER_CHANGE_TOPIC = "user-change";
    private static final String CONSUMER_GROUP_ID = "mc-account";

    private final AccountService accountService;

    @KafkaListener(
            topics = USER_REGISTERED_TOPIC,
            groupId = CONSUMER_GROUP_ID
    )
    public void handleUserRegistered(UserRegisteredEventDto event) {
        log.info(
                "Received user registered event for userId: {}",
                event == null ? null : event.getUserId()
        );

        accountService.handleUserRegistered(event);
    }

    @KafkaListener(
            topics = USER_CHANGE_TOPIC,
            groupId = CONSUMER_GROUP_ID
    )
    public void handleUserChanged(UserChangeEvent event) {
        log.info(
                "Received user change event for userId: {}",
                event == null ? null : event.userId()
        );

        accountService.handleUserChanged(event);
    }
}