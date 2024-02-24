package com.backendemailservice.backendemailservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.backendemailservice.backendemailservice.entity.Email;

@Repository
public interface EmailRepository extends JpaRepository<Email, Integer>{

	@Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail")
    List<Email> loadInbox(String receiverEmail);
	
	@Query("SELECT e FROM Email e WHERE e.sender = :senderEmail")
    List<Email> loadOutbox(String senderEmail);
}
