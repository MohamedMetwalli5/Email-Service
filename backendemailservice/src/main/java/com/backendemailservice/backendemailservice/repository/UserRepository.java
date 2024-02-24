package com.backendemailservice.backendemailservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.backendemailservice.backendemailservice.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
	
	@Query("SELECT u FROM User u WHERE u.email = :email AND u.password = :password")
    Optional<User> findUserByEmailAndPassword(String email, String password);
}
