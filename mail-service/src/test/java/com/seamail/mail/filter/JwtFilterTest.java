package com.seamail.mail.filter;

import com.seamail.mail.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Unit test for JwtFilter using a real JwtUtil (test secret injected via reflection)
// so that malformed/expired token handling is exercised against real parsing behaviour.
@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    private JwtUtil jwtUtil;
    private JwtFilter jwtFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        Field secretField = JwtUtil.class.getDeclaredField("SECRET_KEY");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, "TestJwtSecretKeyForTestingOnlyyyyyyyyyyyyyyyyyy");
        jwtFilter = new JwtFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // SecurityContextHolder is thread-local and reused across test classes on the
        // same thread. Clear it so a context populated here never leaks into other
        // tests (the JwtFilter only sets auth when the context is null).
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotThrowAndShouldContinueChainWhenTokenIsMalformed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer not.a.valid.jwt");

        // Must not throw - a bad token should let the chain continue (-> 401), not 500.
        assertDoesNotThrow(() -> jwtFilter.doFilterInternal(request, response, filterChain));

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldContinueChainWithoutAuthWhenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSetAuthenticationForValidToken() throws Exception {
        String email = "user@seamail.com";
        String token = jwtUtil.generateToken(email);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
