package com.backendemailservice.backendemailservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendemailservice.backendemailservice.entity.Email;


public interface EmailRepository extends JpaRepository<Email, Integer>{

}
