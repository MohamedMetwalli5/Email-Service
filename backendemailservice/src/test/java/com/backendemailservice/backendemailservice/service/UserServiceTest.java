package com.backendemailservice.backendemailservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.backendemailservice.backendemailservice.exception.InvalidFileFormatException;
import com.backendemailservice.backendemailservice.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.UserRepository;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
    }

    @Test
    public void testCreateUser() {
        User user = new User("Nadia@seamail.com", "password");
        userService.createUser(user);

        Optional<User> foundUser = userService.findUser("Nadia@seamail.com", "password");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("Nadia@seamail.com");
    }

    @Test
    public void testFindAndValidateUser() {
        User user = new User("mohamed@seamail.com", passwordEncoder.encode("password"));
        userRepository.save(user);

        Optional<User> foundUser = userService.findAndValidateUser("mohamed@seamail.com", "password");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("mohamed@seamail.com");
    }

    @Test
    public void testNotFoundUser() {
        Optional<User> foundUser = userService.findUser("nonexistent@seamail.com", "wrongpassword");
        assertThat(foundUser).isNotPresent();
    }

    @Test
    public void testDeleteUserAccount() {
        String email = "delete@seamail.com";
        userRepository.save(new User(email, "pass"));

        userService.deleteUserAccount(email);

        assertThat(userRepository.findById(email)).isNotPresent();
    }

    @Test
    public void testChangeUserPassword_Success() {
        String email = "change@seamail.com";
        userRepository.save(new User(email, passwordEncoder.encode("oldPass")));

        userService.changeUserPassword(email, "newPass123");

        User updatedUser = userRepository.findById(email).get();
        assertThat(passwordEncoder.matches("newPass123", updatedUser.getPassword())).isTrue();
    }

    @Test
    public void testUpdateLanguage_Success() {
        String email = "lang@seamail.com";
        userRepository.save(new User(email, "pass"));

        userService.updateLanguage(email, "fr");

        User updatedUser = userRepository.findById(email).get();
        assertThat(updatedUser.getLanguage()).isEqualTo("fr");
    }

    @Test
    public void testUploadProfilePicture_Success_Png() {
        String email = "pic@seamail.com";
        userRepository.save(new User(email, "pass"));

        // Valid PNG Header: 89 50 4E 47
        byte[] validPng = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 0, 0, 0, 0};

        boolean result = userService.uploadProfilePicture(email, validPng);

        assertThat(result).isTrue();
        assertThat(userRepository.findById(email).get().getProfilePicture()).isEqualTo(validPng);
    }

    @Test
    public void testUploadProfilePicture_InvalidFormat_ThrowsException() {
        String email = "bad@seamail.com";
        userRepository.save(new User(email, "pass"));

        byte[] invalidData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8}; // Not PNG or JPEG

        assertThrows(InvalidFileFormatException.class, () -> {
            userService.uploadProfilePicture(email, invalidData);
        });
    }

    @Test
    public void testUploadProfilePicture_UserNotFound_ThrowsException() {
        byte[] validPng = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 0, 0, 0, 0};

        assertThrows(UserNotFoundException.class, () -> {
            userService.uploadProfilePicture("ghost@seamail.com", validPng);
        });
    }

    @Test
    public void testFetchProfilePicture_Success() {
        String email = "fetch@seamail.com";
        byte[] mockPic = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        User user = new User(email, "pass");
        user.setProfilePicture(mockPic);
        userRepository.save(user);

        byte[] fetchedPic = userService.fetchProfilePicture(email);

        assertThat(fetchedPic).isEqualTo(mockPic);
    }
}