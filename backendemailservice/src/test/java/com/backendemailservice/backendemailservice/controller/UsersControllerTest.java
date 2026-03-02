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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false) // Disables Spring Security filter chain
public class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private EmailService emailService;

    @SpyBean
    private JwtUtil jwtUtil;

    private final String dummyToken = "Bearer dummy-token";


    @Test
    public void testDeleteAccount_Unauthorized() throws Exception {
        doReturn(null).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(delete("/api/v1/delete-account")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testChangePassword_Unauthorized() throws Exception {
        String validFormatPassword = "longEnoughPassword123";
        doReturn("other@user.com").when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(put("/api/v1/change-password")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\",\"newPassword\":\"" + validFormatPassword + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateLanguage_Unauthorized() throws Exception {
        doReturn(null).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(put("/api/v1/update-language")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\",\"language\":\"en\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUploadProfilePicture_Unauthorized() throws Exception {
        doReturn(null).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(post("/api/v1/test@seamail.com/profile-picture")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(new byte[]{1, 2, 3}))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetProfilePicture_Unauthorized() throws Exception {
        doReturn(null).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(get("/api/v1/test@seamail.com/profile-picture")
                .header("Authorization", dummyToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testChangePassword_InvalidData_ReturnsBadRequest() throws Exception {
        // This tests that @Valid on the DTO rejects passwords that are too short
        mockMvc.perform(put("/api/v1/change-password")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    public void testDeleteAccount_Success() throws Exception {
        String email = "test@seamail.com";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(delete("/api/v1/delete-account")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account is deleted!"));

        verify(userService, times(1)).deleteUserAccount(email);
    }

    @Test
    public void testChangePassword_Success() throws Exception {
        String email = "test@seamail.com";
        String newPass = "newSecurePass123";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(put("/api/v1/change-password")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"newPassword\":\"" + newPass + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully!"));

        verify(userService, times(1)).changeUserPassword(eq(email), eq(newPass));
    }

    @Test
    public void testUpdateLanguage_Success() throws Exception {
        String email = "test@seamail.com";
        String lang = "fr";
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(put("/api/v1/update-language")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"language\":\"" + lang + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Language updated successfully!"));

        verify(userService, times(1)).updateLanguage(email, lang);
    }

    @Test
    public void testUploadProfilePicture_Success() throws Exception {
        String email = "test@seamail.com";
        byte[] imageData = new byte[]{1, 2, 3};
        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());

        mockMvc.perform(post("/api/v1/" + email + "/profile-picture")
                .header("Authorization", dummyToken)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(imageData))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile picture uploaded successfully."));

        verify(userService, times(1)).uploadProfilePicture(eq(email), any(byte[].class));
    }

    @Test
    public void testGetProfilePicture_Success() throws Exception {
        String email = "test@seamail.com";
        byte[] mockImageData = new byte[]{10, 20, 30};

        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());
        when(userService.fetchProfilePicture(email)).thenReturn(mockImageData);

        mockMvc.perform(get("/api/v1/" + email + "/profile-picture")
                .header("Authorization", dummyToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(mockImageData));
    }

    @Test
    public void testGetProfilePicture_NotFound() throws Exception {
        String email = "test@seamail.com";

        doReturn(email).when(jwtUtil).extractAndValidateToken(anyString());
        when(userService.fetchProfilePicture(email)).thenReturn(null);

        mockMvc.perform(get("/api/v1/" + email + "/profile-picture")
                .header("Authorization", dummyToken))
                .andExpect(status().isNotFound());
    }
}