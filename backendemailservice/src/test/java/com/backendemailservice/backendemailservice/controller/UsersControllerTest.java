package com.backendemailservice.backendemailservice.controller;

import com.backendemailservice.backendemailservice.service.EmailService;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private EmailService emailService;

    @SpyBean
    private JwtUtil jwtUtil;

    @Test
    public void testDeleteAccount_Unauthorized() throws Exception {
        mockMvc.perform(post("/deleteaccount")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testChangePassword_Unauthorized() throws Exception {
        mockMvc.perform(put("/changepassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\",\"newPassword\":\"newpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateLanguage_Unauthorized() throws Exception {
        mockMvc.perform(put("/updatelanguage")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\",\"language\":\"en\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUploadProfilePicture_Unauthorized() throws Exception {
        mockMvc.perform(post("/test@seamail.com/profile-picture")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(new byte[]{1, 2, 3}))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetProfilePicture_Unauthorized() throws Exception {
        mockMvc.perform(get("/test@seamail.com/profile-picture"))
                .andExpect(status().isUnauthorized());
    }
}