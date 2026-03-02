package com.backendemailservice.backendemailservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.EmailRepository;

@Service
public class EmailService {

	@Autowired
	private EmailRepository repository;

	@Autowired
	private CacheManager cacheManager;

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

	public void deleteEmail(Integer emailID) {
		Email email = repository.findById(emailID).orElseThrow();
		repository.deleteById(emailID);
		Cache cache = cacheManager.getCache("inbox");
		if (cache != null) {
			cache.evict(email.getReceiver());
		}
	}

	@CacheEvict(value = "inbox", key = "#receiver")
	public void evictInboxCache(String receiver) {}

	@Transactional
	public void moveToTrashBox(Integer emailID) {
		Email email = repository.findById(emailID).orElseThrow();
		repository.moveToTrashBox(emailID);
		Cache cache = cacheManager.getCache("inbox");
		if (cache != null) {
			cache.evict(email.getReceiver());
		}
	}

	public List<Email> sortEmails(String receiverEmail, String sortingOption) {
		if (sortingOption.equals("priority")) {
			return repository.sortEmailsByPriority(receiverEmail);
		} else {
			return repository.sortEmailsByDate(receiverEmail);
		}
	}

	public List<Email> filterEmails(String receiverEmail, String filteringOption, String filteringValue) {
		if (filteringOption.equals("subject")) {
			return repository.filterEmailsBySubject(receiverEmail, filteringValue);
		} else {
			return repository.filterEmailsBySender(receiverEmail, filteringValue);
		}
	}

}
