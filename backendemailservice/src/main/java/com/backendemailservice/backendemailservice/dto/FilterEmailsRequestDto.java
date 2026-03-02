package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FilterEmailsRequestDto {

    @NotBlank(message = "Filtering option must not be blank")
    @Pattern(regexp = "^(subject|sender)$", message = "Filtering option must be 'subject' or 'sender'")
    private String filteringOption;

    @NotBlank(message = "Filtering value must not be blank")
    @Size(max = 255, message = "Filtering value must not exceed 255 characters")
    private String filteringValue;

    public FilterEmailsRequestDto() {}

    public String getFilteringOption() { return filteringOption; }
    public void setFilteringOption(String filteringOption) { this.filteringOption = filteringOption; }

    public String getFilteringValue() { return filteringValue; }
    public void setFilteringValue(String filteringValue) { this.filteringValue = filteringValue; }
}