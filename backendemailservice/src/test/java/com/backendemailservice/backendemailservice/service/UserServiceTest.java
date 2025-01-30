package com.backendemailservice.backendemailservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.repository.UserRepository;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test") // Activating the "test" profile to use the application-test.properties files
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @BeforeEach
    public void setUp() {
        userRepository.deleteAll(); // Clearing the repository before each test
    }

    @Test
    public void testCreateUser() {
        User user = new User("Nadia@example.com", "password");
        
        userService.createUser(user);

        Optional<User> foundUser = userService.findUser("Nadia@example.com", "password");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("Nadia@example.com");
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
        Optional<User> foundUser = userService.findUser("nonexistent@example.com", "wrongpassword");
        assertThat(foundUser).isNotPresent();
    }
}