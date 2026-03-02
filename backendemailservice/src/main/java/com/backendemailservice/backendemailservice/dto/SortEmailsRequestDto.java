package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SortEmailsRequestDto {

    @NotBlank(message = "Sorting option must not be blank")
    @Pattern(regexp = "^(priority|date)$", message = "Sorting option must be 'priority' or 'date'")
    private String sortingOption;

    public SortEmailsRequestDto() {}

    public String getSortingOption() { return sortingOption; }
    public void setSortingOption(String sortingOption) { this.sortingOption = sortingOption; }
}