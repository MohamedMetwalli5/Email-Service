package com.backendemailservice.backendemailservice.service;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import java.util.Optional;


@Service
public class UserService {

	@Autowired
	private UserRepository repository;
	
//	public User createUser(User user) {
//		return repository.save(user);
//	}

    public Optional<User> findUser(String email, String password) {
        return repository.findUser(email, password);
    }
}
