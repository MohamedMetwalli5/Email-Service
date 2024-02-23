package com.emailservice.backendemailservice;

public class Email {

	private String sender;
	private String receiver;
	private String subject;
	private String priority;
	private String date;
	private String text;
	
	
	public Email(String sender, String receiver, String subject, String priority, String date, String text) {
		this.sender = sender;
		this.receiver = receiver;
		this.subject = subject;
		this.priority = priority;
		this.date = date;
		this.text = text;
	}
	
	
}
