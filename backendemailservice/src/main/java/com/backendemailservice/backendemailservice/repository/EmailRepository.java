package com.backendemailservice.backendemailservice.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.backendemailservice.backendemailservice.entity.Email;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false")
    Page<Email> loadInbox(String receiverEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false")
    Page<Email> loadOutbox(String senderEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true")
    Page<Email> loadTrashbox(String receiverEmail, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Email e SET e.trash = true WHERE e.emailID = :emailID AND e.receiver = :userEmail")
    void moveToTrashBox(Long emailID, String userEmail);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false ORDER BY e.priority ASC")
    Page<Email> sortEmailsByPriority(String receiverEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false ORDER BY e.date ASC")
    Page<Email> sortEmailsByDate(String receiverEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false ORDER BY e.priority ASC")
    Page<Email> sortOutboxByPriority(String senderEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false ORDER BY e.date ASC")
    Page<Email> sortOutboxByDate(String senderEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true ORDER BY e.priority ASC")
    Page<Email> sortTrashboxByPriority(String receiverEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true ORDER BY e.date ASC")
    Page<Email> sortTrashboxByDate(String receiverEmail, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false AND e.subject = :subjectEmailOption")
    Page<Email> filterEmailsBySubject(String receiverEmail, String subjectEmailOption, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false AND e.sender = :senderEmailOption")
    Page<Email> filterEmailsBySender(String receiverEmail, String senderEmailOption, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false AND e.subject = :subject")
    Page<Email> filterOutboxBySubject(String senderEmail, String subject, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false AND e.receiver = :receiver")
    Page<Email> filterOutboxByReceiver(String senderEmail, String receiver, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true AND e.subject = :subject")
    Page<Email> filterTrashBySubject(String receiverEmail, String subject, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true AND e.sender = :sender")
    Page<Email> filterTrashBySender(String receiverEmail, String sender, Pageable pageable);
}
