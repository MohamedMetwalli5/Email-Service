package com.backendemailservice.backendemailservice.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.FilteringWrapper;
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

	@Transactional
	public void moveToTrashBox(Email email) {
		repository.moveToTrashBox(email.getEmailID());
	}

	public List<Email> sortEmails(SortingWrapper sortingWrapper) {
		if(sortingWrapper.getSortingOption().equals("priority")) {
			return repository.sortEmailsByPriority(sortingWrapper.getUser().getEmail());	
		}else {
			return repository.sortEmailsByDate(sortingWrapper.getUser().getEmail());			
		}
	}

	public List<Email> filterEmails(FilteringWrapper filteringWrapper) {
		if(filteringWrapper.getFilteringOption().equals("subject")) {
			return repository.filterEmailsBySubject(filteringWrapper.getUser().getEmail(), filteringWrapper.getFilteringValue());
		}else {
			return repository.filterEmailsBySender(filteringWrapper.getUser().getEmail(), filteringWrapper.getFilteringValue());			
		}
	}

}
