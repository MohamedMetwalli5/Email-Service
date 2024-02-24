package com.backendemailservice.backendemailservice.controller;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.UserService;

@RestController
public class AccessController {

    private final UserService userService;

    @Autowired
    public AccessController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signin")
    public ResponseEntity<String> signin(@RequestBody User user) {
        // Logic to use the implemented user service to check the database for the existence of a user with both email and password
        Optional<User> foundUser = userService.findUser(user.getEmail(), user.getPassword());
        if (foundUser.isPresent()) {
            // User exists in the database
            return ResponseEntity.ok().body("User found: " + user.getEmail());
        } else {
            // User does not exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        Optional<User> foundUser = userService.findUser(user.getEmail(), user.getPassword());
        if (!foundUser.isPresent()) {
            // User doesn't exist in the database
        	userService.createUser(user);
            return ResponseEntity.ok().body("User created");
        } else {
            // User exists
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User already exists");
        }
    }
}
