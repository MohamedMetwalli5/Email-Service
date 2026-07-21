package com.seamail.auth.controller;

import com.seamail.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// Service-to-service endpoint, reachable only inside the container network:
// the gateway deliberately has no route for /internal/**.
@RestController
public class InternalUsersController {

    private final UserRepository userRepository;

    public InternalUsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/internal/users/{email}/exists")
    public ResponseEntity<Void> existsByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email).isPresent()
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
