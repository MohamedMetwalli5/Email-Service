package com.backendemailservice.backendemailservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.backendemailservice.backendemailservice.entity.Email;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long>{

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false")
    List<Email> loadInbox(String receiverEmail);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false")
    List<Email> loadOutbox(String senderEmail);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true")
    List<Email> loadTrashbox(String receiverEmail);

    // add @Transactional; add clearAutomatically + flushAutomatically
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Email e SET e.trash = true WHERE e.emailID = :emailID")
    void moveToTrashBox(Long emailID);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false ORDER BY e.priority ASC")
    List<Email> sortEmailsByPriority(String receiverEmail);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = false ORDER BY e.date ASC")
    List<Email> sortEmailsByDate(String receiverEmail);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false ORDER BY e.priority ASC")
    List<Email> sortOutboxByPriority(String senderEmail);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.trash = false ORDER BY e.date ASC")
    List<Email> sortOutboxByDate(String senderEmail);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true ORDER BY e.priority ASC")
    List<Email> sortTrashboxByPriority(String receiverEmail);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true ORDER BY e.date ASC")
    List<Email> sortTrashboxByDate(String receiverEmail);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.subject = :subjectEmailOption")
    List<Email> filterEmailsBySubject(String receiverEmail, String subjectEmailOption);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.sender = :senderEmailOption")
    List<Email> filterEmailsBySender(String receiverEmail, String senderEmailOption);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.subject = :subject")
    List<Email> filterOutboxBySubject(String senderEmail, String subject);

    @Query("SELECT e FROM Email e WHERE e.sender = :senderEmail AND e.receiver = :receiver")
    List<Email> filterOutboxByReceiver(String senderEmail, String receiver);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true AND e.subject = :subject")
    List<Email> filterTrashBySubject(String receiverEmail, String subject);

    @Query("SELECT e FROM Email e WHERE e.receiver = :receiverEmail AND e.trash = true AND e.sender = :sender")
    List<Email> filterTrashBySender(String receiverEmail, String sender);
}
