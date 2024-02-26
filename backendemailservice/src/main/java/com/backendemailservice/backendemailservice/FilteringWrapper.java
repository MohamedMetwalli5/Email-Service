package com.backendemailservice.backendemailservice;

import com.backendemailservice.backendemailservice.entity.User;

public class FilteringWrapper {

	private User user;
	private String filteringOption;
	private String filteringValue;
	
	public User getUser() {
		return user;
	}
	
	public String getFilteringOption() {
		return filteringOption;
	}
	
	public String getfilteringValue() {
		return filteringValue;
	}
	
}
