package com.backendemailservice.backendemailservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendemailservice.backendemailservice.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
	

}
