package com.backendemailservice.backendemailservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "emails")
public class Email {
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
	private String date;
	
	
	public Email(String sender, String receiver, String subject, String body, String priority, String date) {
		this.sender = sender;
		this.receiver = receiver;
		this.subject = subject;
		this.body = body;
		this.priority = priority;
		this.date = date;
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

	public String getDate() {
		return date;
	}
	

	public Email() {
		
	}	
	
}
