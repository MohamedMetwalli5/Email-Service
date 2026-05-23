package com.backendemailservice.backendemailservice.repository;

import com.backendemailservice.backendemailservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Repository slice test using @DataJpaTest
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        User user = new User("test@seamail.com", "encodedPassword");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@seamail.com");

        assertTrue(found.isPresent());
        assertEquals("test@seamail.com", found.get().getEmail());
        assertEquals("encodedPassword", found.get().getPassword());
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@seamail.com");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindUserByFoundReceiverQuery() {
        User user = new User("receiver@seamail.com", "pass");
        userRepository.save(user);

        Optional<User> found = userRepository.foundReceiver("receiver@seamail.com");

        assertTrue(found.isPresent());
        assertEquals("receiver@seamail.com", found.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenReceiverNotFound() {
        Optional<User> found = userRepository.foundReceiver("missing@seamail.com");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldDeleteUserById() {
        User user = new User("delete@seamail.com", "pass");
        userRepository.save(user);

        assertTrue(userRepository.findById("delete@seamail.com").isPresent());

        userRepository.deleteById("delete@seamail.com");

        assertFalse(userRepository.findById("delete@seamail.com").isPresent());
    }

    @Test
    void shouldSaveUserWithLanguageAndProfilePicture() {
        User user = new User("full@seamail.com", "pass");
        user.setLanguage("fr");
        user.setProfilePicture(new byte[]{1, 2, 3});
        userRepository.save(user);

        User found = userRepository.findById("full@seamail.com").orElseThrow();

        assertEquals("fr", found.getLanguage());
        assertArrayEquals(new byte[]{1, 2, 3}, found.getProfilePicture());
    }
}
