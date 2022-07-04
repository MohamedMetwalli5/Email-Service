package com.emailservice.backendemailservice;

public class Email {

	private String sender;
	private String subject;
	private String priority;
	private String date;
	
	public Email() {
		
	}
	
	public Email(String sender, String subject, String priority, String date) {
		this.sender = sender;
		this.subject = subject;
		this.priority = priority;
		this.date = date;
	}
	
	
}
