package com.backendemailservice.backendemailservice.service;

import com.backendemailservice.backendemailservice.exception.UserNotFoundException;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.UserRepository;

import jakarta.transaction.Transactional;

import java.util.Optional;


@Service
public class UserService {

	@Autowired
	private UserRepository repository;

	@Autowired
	private PasswordEncoder passwordEncoder;
    
	public void createUser(User user) {
		String saltedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(saltedPassword);
		repository.save(user);
	}

	public Optional<User> findAndValidateUser(String email, String password) {
	    Optional<User> optionalUser = repository.findByEmail(email);
	    if (optionalUser.isPresent()) {
	        User user = optionalUser.get();
	        if (passwordEncoder.matches(password, user.getPassword())) {
	            return Optional.of(user);
	        }
	    }
	    return Optional.empty();
	}
	
    public Optional<User> findUser(String email, String password) {
    	return findAndValidateUser(email, password);
    }
    
    public Optional<User> foundReceiver(String email) {
    	return repository.foundReceiver(email);
    }

	public void deleteUserAccount(String userEmail) {
		repository.deleteById(userEmail);
	}

	public void changeUserPassword(String userEmail, String newPassword) {
		User user = repository.findByEmail(userEmail)
				.orElseThrow(() -> new UserNotFoundException("User not found."));
		user.setPassword(passwordEncoder.encode(newPassword));
		repository.save(user);
	}

	public void updateLanguage(String email, String language) {
		User user = repository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found."));
		user.setLanguage(language);
		repository.save(user);
	}

	@Transactional
	public boolean uploadProfilePicture(String email, byte[] profilePicture) {
		User user = repository.findById(email)
				.orElseThrow(() -> new UserNotFoundException("User not found."));
		user.setProfilePicture(profilePicture);
		repository.save(user);
		return true;
	}

	@Transactional
	public byte[] fetchProfilePicture(String email) {
		User user = repository.findById(email)
				.orElseThrow(() -> new UserNotFoundException("User not found."));
		return user.getProfilePicture();
	}
	
}