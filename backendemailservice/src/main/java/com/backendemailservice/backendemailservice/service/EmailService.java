package com.backendemailservice.backendemailservice.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backendemailservice.backendemailservice.entity.Email;

@RestController
public class EmailService {

	@RequestMapping("/emails")
	public List<Email> RequestHandler(String param) {
		if(param.equals("Inbox")) {
			return Arrays.asList(
					new Email("mohamed", "ali","assignment", "1", "7/4/2022", "hello ali!"),
					new Email("ali",  "ahmed", "club", "2", "9/6/2022", "hello ahmed!")
			);
		}
		
		return null;
	}
}
