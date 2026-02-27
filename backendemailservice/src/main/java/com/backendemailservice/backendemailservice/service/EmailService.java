package com.backendemailservice.backendemailservice.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backendemailservice.backendemailservice.FilteringWrapper;
import com.backendemailservice.backendemailservice.SortingWrapper;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.EmailRepository;

@Service
public class EmailService {

	@Autowired
	private EmailRepository repository;

	@Cacheable(value = "inbox", key = "#user.getEmail()")
	public List<Email> loadInbox(User user){
		return repository.loadInbox(user.getEmail());
	}
	
	public List<Email> loadOutbox(User user){
		return repository.loadOutbox(user.getEmail());
	}

	public List<Email> loadTrashbox(User user) {
		return repository.loadTrashbox(user.getEmail());
	}

	@CacheEvict(value = "inbox", key = "#email.getReceiver()")
	public void createEmail(Email email) {
		repository.save(email);
	}

	public void deleteEmail(Email email) {
		repository.deleteById(email.getEmailID());
	}

	@Transactional
	@CacheEvict(value = "inbox", key = "#email.getReceiver()")
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
