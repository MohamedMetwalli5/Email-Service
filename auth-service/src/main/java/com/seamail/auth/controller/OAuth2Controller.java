package com.seamail.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seamail.auth.service.IUserService;
import java.util.Map;



@RestController
@RequestMapping("/api/v1")
@Validated
public class OAuth2Controller {

    @Value("${cors.allowed.origin}")
    private String allowedOrigin;

    private final IUserService userService;

    public OAuth2Controller(IUserService userService) {
        this.userService = userService;
    }

    // Returns a CSRF state nonce for the Discord OAuth flow.
    // The frontend fetches this before redirecting to Discord.
    @GetMapping("/auth/discord/state")
    public ResponseEntity<Map<String, String>> generateState() {
        String state = userService.generateDiscordState();
        return ResponseEntity.ok(Map.of("state", state));
    }

    @GetMapping("/auth/discord")
    public ResponseEntity<Void> handleDiscordLogin(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {

        String redirectUrl = userService.processDiscordOAuth(code, state, allowedOrigin);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }
}
