package com.backendemailservice.backendemailservice.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "emails")
public class Email implements Serializable {
	@Id
	@Column(name="emailID", nullable = false, unique = true)
	@GeneratedValue
	private Integer emailID;
	
	@Column(name="sender", nullable = false)
	private String sender;
	
	@Column(name="receiver", nullable = false)
	private String receiver;
	
	@Column(name="subject", nullable = false)
	private String subject;
	
	@Column(name="body", nullable = false)
	private String body;
	
	@Column(name="priority", nullable = false)
	private String priority;
	
	@Column(name="date", nullable = false)
	private LocalDateTime date;
	
	@Column(name="trash", nullable = false)
	private boolean trash;
	
	
	public Email(String sender, String receiver, String subject, String body, String priority, LocalDateTime date, boolean trash) {
		this.sender = sender;
		this.receiver = receiver;
		this.subject = subject;
		this.body = body;
		this.priority = priority;
		this.date = date;
		this.trash = trash;
	}
	
	public Integer getEmailID() {
		return emailID;
	}

	public String getSender() {
		return sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public String getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public String getPriority() {
		return priority;
	}

	public LocalDateTime getDate() {
		return date;
	}
	
	public boolean isTrash() {
		return trash;
	}

	public void setSender(String sender) { this.sender = sender; }

	public void setReceiver(String receiver) { this.receiver = receiver; }

	public void setSubject(String subject) { this.subject = subject; }

	public void setBody(String body) { this.body = body; }

	public void setPriority(String priority) { this.priority = priority; }

	public void setDate(LocalDateTime date) { this.date = date; }

	public void setTrash(boolean trash) { this.trash = trash; }

	public Email() {
		
	}	
	
}
