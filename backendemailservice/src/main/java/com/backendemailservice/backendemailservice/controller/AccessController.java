package com.backendemailservice.backendemailservice.controller;

import java.util.Map;
import java.util.Optional;

import com.backendemailservice.backendemailservice.dto.UserRequestDto;
import com.backendemailservice.backendemailservice.exception.InvalidEmailDomainException;
import com.backendemailservice.backendemailservice.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.backendemailservice.backendemailservice.exception.UserNotFoundException;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.*;

@RestController
@RequestMapping("/api/v1")
public class AccessController {
	
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AccessController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/sign-in")
    public ResponseEntity<String> signin(@Valid @RequestBody UserRequestDto userRequestDto) {
        Optional<User> foundUser = userService.findUser(userRequestDto.getEmail(), userRequestDto.getPassword());
        if (foundUser.isPresent()) {
            String token = jwtUtil.generateToken(userRequestDto.getEmail());
            return ResponseEntity.ok().body("Bearer " + token);
        }
        throw new UserNotFoundException("User not found");
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signup(@Valid @RequestBody UserRequestDto userRequestDto) {
        if (userService.findUser(userRequestDto.getEmail(), userRequestDto.getPassword()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }

        if (!userRequestDto.getEmail().endsWith("@seamail.com")) {
            throw new InvalidEmailDomainException("Emails must end with @seamail.com");
        }

        User user = new User(userRequestDto.getEmail(), userRequestDto.getPassword());
        userService.createUser(user);

        String token = jwtUtil.generateToken(userRequestDto.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "User created successfully",
                    "token", token
                ));
    }
}