package com.backendemailservice.backendemailservice;

import com.backendemailservice.backendemailservice.entity.User;

public class SortingWrapper {

    private User user;
    
    private String sortingOption;
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getSortingOption() {
        return sortingOption;
    }
    
    public void setSortingOption(String sortingOption) {
        this.sortingOption = sortingOption;
    }
}