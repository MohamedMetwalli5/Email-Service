package com.backendemailservice.backendemailservice.controller;

import java.util.Map;
import java.util.Optional;

import com.backendemailservice.backendemailservice.dto.UserRequestDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.*;

@RestController
public class AccessController {
	
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AccessController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/signin")
    public ResponseEntity<String> signin(@Valid @RequestBody UserRequestDto userRequestDto) {
        Optional<User> foundUser = userService.findUser(userRequestDto.getEmail(), userRequestDto.getPassword());
        if (foundUser.isPresent()) {
            String token = jwtUtil.generateToken(userRequestDto.getEmail());
            return ResponseEntity.ok().body("Bearer " + token);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody UserRequestDto userRequestDto) {
        if (userRequestDto.getEmail() == null || userRequestDto.getEmail().isEmpty() || userRequestDto.getPassword() == null || userRequestDto.getPassword().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email and password must not be empty"));
        }

        if (userService.findUser(userRequestDto.getEmail(), userRequestDto.getPassword()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "User already exists"));
        }
        
        if (!userRequestDto.getEmail().endsWith("@seamail.com")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Emails must end with @seamail.com"));
        }

        User user = new User(userRequestDto.getEmail(), userRequestDto.getPassword());
        userService.createUser(user);

        String token = jwtUtil.generateToken(userRequestDto.getEmail());

        // Returning the response with JWT
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "User created successfully",
                    "token", token
                ));
    }


    
}
