package com.munitax.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.auth.model.User;
import com.munitax.auth.repository.UserRepository;
import com.munitax.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("test-id-123");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhoneNumber("555-1234");
        testUser.addRole(User.UserRole.ROLE_INDIVIDUAL);
        testUser.setActive(true);
    }

    @Test
    void testLogin_Success() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername("test@example.com")
            .password(testUser.getPasswordHash())
            .authorities("ROLE_INDIVIDUAL")
            .build();
        
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);

        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());

        String loginRequest = """
            {
                "email": "wrong@example.com",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_WrongPassword() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegister_Success() throws Exception {
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        String registerRequest = """
            {
                "email": "newuser@example.com",
                "password": "password123",
                "firstName": "New",
                "lastName": "User",
                "roles": ["ROLE_INDIVIDUAL"]
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated());
    }

    @Test
    void testRegister_DuplicateEmail() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        String registerRequest = """
            {
                "email": "test@example.com",
                "password": "password123",
                "firstName": "Test",
                "lastName": "User",
                "roles": ["ROLE_INDIVIDUAL"]
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidateToken_ValidToken() throws Exception {
        // This test would require a valid JWT token
        // Implementation depends on token validation endpoint
    }

    @Test
    void testValidateToken_InvalidToken() throws Exception {
        // This test would require an invalid JWT token
        // Implementation depends on token validation endpoint
    }
}
