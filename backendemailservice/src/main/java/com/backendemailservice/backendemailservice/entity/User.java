package com.backendemailservice.backendemailservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@Column(name="email", nullable = false, unique = true)
	private String email;
	
	@Column(name="password", nullable = false)
	private String password;
	
	@Column(name = "language")
    private String language;
	
	@Lob
	@Column(columnDefinition = "MEDIUMBLOB")
    private byte[] profilePicture;
	
	public User(String email, String password){
		this.email = email;
		this.password = password;
	}
	 
	public User() {
		
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getLanguage() {
        return language;
    }
	
	public byte[] getProfilePicture() {
        return profilePicture;
    }
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setLanguage(String language) {
        this.language = language;
    }
	
	public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }
}

