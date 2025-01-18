package com.backendemailservice.backendemailservice;

import com.backendemailservice.backendemailservice.entity.User;

public class FilteringWrapper {

    private User user;
    
    private String filteringOption;
    private String filteringValue;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFilteringOption() {
        return filteringOption;
    }

    public void setFilteringOption(String filteringOption) {
        this.filteringOption = filteringOption;
    }

    public String getFilteringValue() {
        return filteringValue;
    }

    public void setFilteringValue(String filteringValue) {
        this.filteringValue = filteringValue;
    }
}
