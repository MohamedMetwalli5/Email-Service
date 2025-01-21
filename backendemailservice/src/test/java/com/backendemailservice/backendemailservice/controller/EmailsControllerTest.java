package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.FilteringWrapper;
import com.backendemailservice.backendemailservice.SortingWrapper;
import com.backendemailservice.backendemailservice.entity.Email;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EmailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @SpyBean
    private JwtUtil jwtUtil;

    @InjectMocks
    private EmailsController emailsController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLoadInbox_Success() throws Exception {
        String email = "testuser@example.com";
        String token = jwtUtil.generateToken(email);

        when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        
        // Setting up the email service mock
        when(emailService.loadInbox(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/inbox")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testLoadInbox_Unauthorized_NoToken() throws Exception {
        mockMvc.perform(post("/inbox"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    public void testSendEmail_Success() throws Exception {
        String email = "example1@seamail.com";
        String receiverEmail = "example9@seamail.com";
        String token = jwtUtil.generateToken(email);

        Email emailObj = new Email(email, receiverEmail, "Subject", "Body", "1", "2023-10-01", "No");
        
        when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
        when(userService.foundReceiver(emailObj.getReceiver())).thenReturn(Optional.of(new User(receiverEmail, "password")));
        
        mockMvc.perform(post("/sendemail")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sender\":\"" + email + "\", \"receiver\":\"" + receiverEmail + "\", \"subject\":\"Subject\", \"body\":\"Body\", \"priority\":\"1\", \"date\":\"2023-10-01\",\"trash\":\"No\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email is sent!"));
    }

    @Test
    public void testLoadOutbox_Success() throws Exception {
        String email = "testuser@example.com";
        String token = jwtUtil.generateToken(email);

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
        when(emailService.loadOutbox(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/outbox")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testSortEmails_Authorized() throws Exception {
        User user = new User("test@example.com", "password");

        String token = jwtUtil.generateToken(user.getEmail());

        SortingWrapper sortingWrapper = new SortingWrapper();
        sortingWrapper.setUser(user);
        sortingWrapper.setSortingOption("priority");

        List<Email> sortedEmails = Arrays.asList(
            new Email("sender1@example.com", "receiver1@example.com", "Subject 1", "Body 1", "1", "2023-10-01", "No"),
            new Email("sender2@example.com", "receiver2@example.com", "Subject 2", "Body 2", "2", "2023-10-02", "No")
        );

        when(jwtUtil.isTokenValid(token, user.getEmail())).thenReturn(true);
        when(emailService.sortEmails(any(SortingWrapper.class))).thenReturn(sortedEmails);

        mockMvc.perform(post("/sortemails")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\":{\"email\":\"test@example.com\"}, \"sortingOption\":\"priority\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSortEmails_Unauthorized_NoToken() throws Exception {
        mockMvc.perform(post("/sortemails")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\":{\"email\":\"test@example.com\"}, \"sortingOption\":\"priority\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFilterEmails_Authorized() throws Exception {
        User user = new User("test@example.com", "password");

        String token = jwtUtil.generateToken(user.getEmail());

        FilteringWrapper filteringWrapper = new FilteringWrapper();
        filteringWrapper.setUser(user);
        filteringWrapper.setFilteringOption("subject");
        filteringWrapper.setFilteringValue("Important");

        List<Email> filteredEmails = Arrays.asList(
            new Email("sender@example.com", "test@example.com", "Important", "This is an important email", "2", "2023-10-01", "No"),
            new Email("sender2@example.com", "test@example.com", "Important Update", "Details on important updates", "1", "2023-10-02", "No")
        );

        when(jwtUtil.isTokenValid(token, user.getEmail())).thenReturn(true);
        when(emailService.filterEmails(any(FilteringWrapper.class))).thenReturn(filteredEmails);

        mockMvc.perform(post("/filteremails")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\":{\"email\":\"test@example.com\"}, \"filteringOption\":\"subject\", \"filteringValue\":\"Important\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testFilterEmails_Unauthorized_NoToken() throws Exception {
        mockMvc.perform(post("/filteremails")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"user\":{\"email\":\"test@example.com\"}, \"filteringOption\":\"subject\", \"filteringValue\":\"Important\"}"))
                .andExpect(status().isForbidden());
    }

}