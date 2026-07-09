package com.backendemailservice.backendemailservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.backendemailservice.backendemailservice.filter.JwtFilter;
import com.backendemailservice.backendemailservice.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity 
@EnableConfigurationProperties(DiscordOAuthProperties.class)
public class SecurityConfig {

    private final JwtFilter jwtFilter; 
    private final CustomUserDetailsService userDetailsService; 
    private final DiscordOAuthProperties discordProperties; // grouped @ConfigurationProperties

    public SecurityConfig(JwtFilter jwtFilter,
                          CustomUserDetailsService userDetailsService,
                          DiscordOAuthProperties discordProperties) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.discordProperties = discordProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); 
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
    .cors(Customizer.withDefaults())
    .csrf(csrf -> csrf.disable())
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/v1/sign-in", "/api/v1/sign-up",
                "/api/v1/auth/discord", "/api/v1/auth/discord/state",
                "/api/v1/auth/refresh", "/api/v1/auth/exchange").permitAll()
        .anyRequest().authenticated()
    )
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    )
    .exceptionHandling(exception -> exception
        .authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Unauthorized\"}");
        })
    )
    .authenticationProvider(authenticationProvider());

    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

}
