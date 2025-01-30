package com.backendemailservice.backendemailservice.service;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import java.util.Optional;


@Service
public class UserService {

	@Autowired
	private UserRepository repository;
	
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
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

	public boolean changeUserPassword(String userEmail, String newPassword) {
        Optional<User> optionalUser = repository.findByEmail(userEmail);
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
    		String saltedNewPassword = passwordEncoder.encode(newPassword);
            user.setPassword(saltedNewPassword);
            repository.save(user);
            return true;
        } else {
            return false;
        }
    }

	public boolean updateLanguage(String email, String language) {
	    Optional<User> optionalUser = repository.findByEmail(email);
	    
	    if (optionalUser.isPresent()) {
	        User user = optionalUser.get();
	        user.setLanguage(language);
	        repository.save(user);
	        return true;
	    }
	    return false;
	}
}
