package com.backendemailservice.backendemailservice.controller;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.*;

@RestController
public class AccessController {
	@Value("${cors.allowed.origin}")
    private String allowedOrigin;
	
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AccessController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    
    @PostMapping("/signin")
    public ResponseEntity<String> signin(@RequestBody User user) {
        Optional<User> foundUser = userService.findUser(user.getEmail(), user.getPassword());
        if (foundUser.isPresent()) {
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok().body("Bearer " + token);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    
//    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty() || user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email and password must not be empty"));
        }

        if (userService.findUser(user.getEmail(), user.getPassword()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "User already exists"));
        }

        userService.createUser(user);

        String token = jwtUtil.generateToken(user.getEmail());

        // Returning the response with JWT
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "User created successfully",
                    "token", token
                ));
    }


    
}
