package com.backendemailservice.backendemailservice.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.SortingWrapper;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.EmailRepository;
import com.backendemailservice.backendemailservice.repository.UserRepository;

@RestController
public class EmailService {

	@Autowired
	private EmailRepository repository;
	
	public List<Email> loadInbox(User user){
		return repository.loadInbox(user.getEmail());
	}
	
	public List<Email> loadOutbox(User user){
		return repository.loadOutbox(user.getEmail());
	}

	public List<Email> loadTrashbox(User user) {
		return repository.loadTrashbox(user.getEmail());
	}

	public void createEmail(Email email) {
		repository.save(email);
	}

	public void deleteEmail(Email email) {
		repository.deleteById(email.getEmailID());
	}

//	public List<Email> sortEmails(SortingWrapper sortingWrapper) {
//		return repository.sortEmails(sortingWrapper.getUser().getEmail(), sortingWrapper.getSortingOption());
//	}

}
