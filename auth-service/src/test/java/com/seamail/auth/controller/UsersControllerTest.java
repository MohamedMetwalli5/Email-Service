package com.seamail.auth.controller;

import com.seamail.auth.config.TestSecurityConfig;
import com.seamail.auth.exception.UserNotFoundException;
import com.seamail.auth.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// V-26: Slice test using @WebMvcTest instead of @SpringBootTest (J-7, MV-1)
@WebMvcTest(UsersController.class)
@Import(TestSecurityConfig.class)
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserService userService;

    private static final String TEST_EMAIL = "test@seamail.com";
    private static final String OTHER_EMAIL = "other@user.com";

    // --- Unauthorized scenarios (different authenticated user) ---

    @Test
    void shouldReturn401WhenDeleteAccountForDifferentUser() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .when(userService).deleteUserAccount(eq(OTHER_EMAIL), eq(TEST_EMAIL));

        mockMvc.perform(delete("/api/v1/delete-account")
                .with(jwt().jwt(j -> j.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenChangePasswordForDifferentUser() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .when(userService).changeUserPassword(eq(OTHER_EMAIL), eq(TEST_EMAIL),
                        anyString(), anyString());

        mockMvc.perform(put("/api/v1/change-password")
                .with(jwt().jwt(j -> j.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\",\"currentPassword\":\"oldPass123\",\"newPassword\":\"longEnoughPassword123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenUpdateLanguageForDifferentUser() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .when(userService).updateLanguage(eq(OTHER_EMAIL), eq(TEST_EMAIL), anyString());

        mockMvc.perform(put("/api/v1/update-language")
                .with(jwt().jwt(j -> j.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\",\"language\":\"en\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenUploadProfilePictureForDifferentUser() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .when(userService).uploadProfilePicture(eq(OTHER_EMAIL), eq(TEST_EMAIL), any(byte[].class));

        mockMvc.perform(post("/api/v1/" + TEST_EMAIL + "/profile-picture")
                .with(jwt().jwt(j -> j.subject(OTHER_EMAIL)))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(new byte[]{1, 2, 3}))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenGetProfilePictureForDifferentUser() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .when(userService).fetchProfilePicture(eq(OTHER_EMAIL), eq(TEST_EMAIL));

        mockMvc.perform(get("/api/v1/" + TEST_EMAIL + "/profile-picture")
                .with(jwt().jwt(j -> j.subject(OTHER_EMAIL))))
                .andExpect(status().isUnauthorized());
    }

    // --- Not found ---

    @Test
    void shouldReturn404WhenProfilePictureUserNotFound() throws Exception {
        when(userService.fetchProfilePicture(TEST_EMAIL, TEST_EMAIL))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/" + TEST_EMAIL + "/profile-picture")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    // --- Success scenarios ---

    @Test
    void shouldReturn204WhenDeleteOwnAccount() throws Exception {
        mockMvc.perform(delete("/api/v1/delete-account")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUserAccount(TEST_EMAIL, TEST_EMAIL);
    }

    @Test
    void shouldReturn204WhenChangeOwnPassword() throws Exception {
        String currentPass = "oldSecurePass123";
        String newPass = "newSecurePass123";

        mockMvc.perform(put("/api/v1/change-password")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\",\"currentPassword\":\"" + currentPass + "\",\"newPassword\":\"" + newPass + "\"}"))
                .andExpect(status().isNoContent());

        verify(userService).changeUserPassword(eq(TEST_EMAIL), eq(TEST_EMAIL),
                eq(currentPass), eq(newPass));
    }

    @Test
    void shouldReturn204WhenUpdateOwnLanguage() throws Exception {
        String lang = "fr";

        mockMvc.perform(put("/api/v1/update-language")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\",\"language\":\"" + lang + "\"}"))
                .andExpect(status().isNoContent());

        verify(userService).updateLanguage(eq(TEST_EMAIL), eq(TEST_EMAIL), eq(lang));
    }

    @Test
    void shouldReturn204WhenUploadOwnProfilePicture() throws Exception {
        byte[] imageData = {1, 2, 3};

        mockMvc.perform(post("/api/v1/" + TEST_EMAIL + "/profile-picture")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL)))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(imageData))
                .andExpect(status().isNoContent());

        verify(userService).uploadProfilePicture(eq(TEST_EMAIL), eq(TEST_EMAIL), any(byte[].class));
    }

    @Test
    void shouldReturn200WithImageWhenGetOwnProfilePicture() throws Exception {
        byte[] mockImageData = {10, 20, 30};

        when(userService.fetchProfilePicture(TEST_EMAIL, TEST_EMAIL)).thenReturn(mockImageData);

        mockMvc.perform(get("/api/v1/" + TEST_EMAIL + "/profile-picture")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(mockImageData));
    }

    // --- Validation ---

    @Test
    void shouldReturn400WhenChangePasswordWithInvalidData() throws Exception {
        mockMvc.perform(put("/api/v1/change-password")
                .with(jwt().jwt(j -> j.subject(TEST_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@seamail.com\",\"currentPassword\":\"\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(userService);
    }

}
