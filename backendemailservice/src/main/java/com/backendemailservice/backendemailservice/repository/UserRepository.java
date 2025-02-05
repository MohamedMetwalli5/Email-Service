package com.backendemailservice.backendemailservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.backendemailservice.backendemailservice.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
	
	@Query("SELECT u FROM User u WHERE u.email = :email")
	Optional<User> foundReceiver(String email);
	
	Optional<User> findByEmail(String email);
}
