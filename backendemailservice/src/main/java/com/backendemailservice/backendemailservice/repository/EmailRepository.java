package com.backendemailservice.backendemailservice.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.backendemailservice.backendemailservice.entity.Email;

@Repository
public interface EmailRepository extends JpaRepository<Email, Integer>{

	@Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = 'No'")
    List<Email> loadInbox(String receiverEmail);
	
	@Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = 'No'")
    List<Email> loadOutbox(String senderEmail);

	@Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = 'Yes'")
	List<Email> loadTrashbox(String receiverEmail);

//	@Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = 'Yes'")
	void moveToTrashBox(Email email);

////////////////// TODO
//	@Query("'SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = 'No' ORDER BY e.sortingOption ASC'")
//	List<Email> sortEmails(String email, String sortingOption);

}

