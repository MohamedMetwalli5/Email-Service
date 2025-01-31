package com.backendemailservice.backendemailservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.backendemailservice.backendemailservice.filter.JwtFilter;

@Configuration
public class SecurityConfig {

    @Value("${discord.client.id}")
    private String discordClientId;

    @Value("${discord.client.secret}")
    private String discordClientSecret;
    
    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    	http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/signin", "/signup", "/DiscordSignin").permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .oauth2Login(Customizer.withDefaults());
        

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(discordClientRegistration());
    }

    private ClientRegistration discordClientRegistration() {
        return ClientRegistration.withRegistrationId("discord") // The provider ID
            .clientId(discordClientId) // The Discord client ID
            .clientSecret(discordClientSecret) // The Discord client secret
            .scope("identify", "email") // Scopes you want to request
            .authorizationUri("https://discord.com/api/oauth2/authorize") // Authorization URL
            .tokenUri("https://discord.com/api/oauth2/token") // Token exchange URL
            .userInfoUri("https://discord.com/api/users/@me") // User info endpoint
            .userNameAttributeName("id") // Specifying the user attribute to use as unique ID
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}") 
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE) // Granting type
            .build();
    }
}