package com.backendemailservice.backendemailservice.controller;

import org.apache.catalina.connector.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.UserService;

@RestController
public class AccessController {
	
	@PostMapping("/") 
	public Response signin(User user) {
		// TODO logic to use an implemented user service to check on the DB for the existence of a user with both email and password
		return UserService.findUser(user);	
	}
	
	@PostMapping("/signup") 
	public Response signup(User user) {
		// TODO logic to use an implemented user service to check on the DB for the existence of a user with this email
		return UserService.createUser(user);	
	}
	
	
	
}
