package com.seamail.mail.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Calls auth-service inside the container network (the gateway has no /internal/** route).
// A 404 response surfaces as FeignException.NotFound.
@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthUserClient {

    @GetMapping("/internal/users/{email}/exists")
    void assertUserExists(@PathVariable String email);
}