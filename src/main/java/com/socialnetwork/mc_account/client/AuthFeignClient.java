package com.socialnetwork.mc_account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "mc-auth",
        url = "${app.auth-service.url:}"
)
public interface AuthFeignClient {

    String AUTHORIZATION_HEADER = "Authorization";

    @GetMapping("/api/v1/auth/validate")
    Boolean validateToken(
            @RequestHeader(AUTHORIZATION_HEADER) String authorizationHeader
    );
}