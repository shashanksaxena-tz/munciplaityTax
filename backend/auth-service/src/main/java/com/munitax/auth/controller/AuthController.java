package com.munitax.auth.controller;

import com.munitax.auth.model.User;
import com.munitax.auth.repository.UserRepository;
import com.munitax.auth.service.CustomUserDetailsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final CustomUserDetailsService userDetailsService;
        private final Key jwtKey;
        private final long jwtExpirationMs;

        public AuthController(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        CustomUserDetailsService userDetailsService,
                        @Value("${jwt.secret:defaultSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm}") String jwtSecret,
                        @Value("${jwt.expiration:86400000}") long jwtExpirationMs) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.userDetailsService = userDetailsService;
                this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
                this.jwtExpirationMs = jwtExpirationMs;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
                try {
                        // Find user by email
                        User user = userRepository.findByEmail(request.email())
                                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

                        // Check if account is active
                        if (!user.isActive()) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(new LoginResponse(null, null, null, null,
                                                                "Account is inactive. Please contact support."));
                        }

                        // Verify password
                        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                                throw new BadCredentialsException("Invalid credentials");
                        }

                        // Load user details and generate token
                        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
                        String token = generateToken(userDetails);

                        // Get roles
                        String roles = userDetails.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .collect(Collectors.joining(","));

                        return ResponseEntity.ok(new LoginResponse(
                                        token,
                                        user.getId(),
                                        user.getEmail(),
                                        roles,
                                        "Login successful"));

                } catch (BadCredentialsException e) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(new LoginResponse(null, null, null, null, "Invalid email or password"));
                }
        }

        @PostMapping("/token")
        public ResponseEntity<TokenResponse> generateToken(@RequestBody TokenRequest request) {
                try {
                        User user = userRepository.findByEmail(request.email())
                                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

                        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                                throw new BadCredentialsException("Invalid credentials");
                        }

                        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
                        String token = generateToken(userDetails);

                        return ResponseEntity.ok(new TokenResponse(token, user.getId()));
                } catch (BadCredentialsException e) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(new TokenResponse(null, null));
                }
        }

        @PostMapping("/validate")
        public ResponseEntity<ValidationResponse> validateToken(@RequestBody ValidationRequest request) {
                try {
                        Jwts.parserBuilder()
                                        .setSigningKey(jwtKey)
                                        .build()
                                        .parseClaimsJws(request.token());

                        return ResponseEntity.ok(new ValidationResponse(true, "Token is valid"));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(new ValidationResponse(false, "Invalid or expired token"));
                }
        }

        @GetMapping("/me")
        public ResponseEntity<UserInfoResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
                try {
                        String token = authHeader.replace("Bearer ", "");
                        String userId = Jwts.parserBuilder()
                                        .setSigningKey(jwtKey)
                                        .build()
                                        .parseClaimsJws(token)
                                        .getBody()
                                        .getSubject();

                        User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        String roles = user.getRoles().stream()
                                        .map(Enum::name)
                                        .collect(Collectors.joining(","));

                        return ResponseEntity.ok(new UserInfoResponse(
                                        user.getId(),
                                        user.getEmail(),
                                        user.getFirstName(),
                                        user.getLastName(),
                                        roles,
                                        user.getTenantId()));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
        }

        private String generateToken(UserDetails userDetails) {
                Date now = new Date();
                Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

                return Jwts.builder()
                                .setSubject(userDetails.getUsername()) // This is the user ID
                                .setIssuedAt(now)
                                .setExpiration(expiryDate)
                                .claim("roles", userDetails.getAuthorities().stream()
                                                .map(GrantedAuthority::getAuthority)
                                                .collect(Collectors.joining(",")))
                                .signWith(jwtKey, SignatureAlgorithm.HS256)
                                .compact();
        }

        // DTOs
        public record LoginRequest(String email, String password) {
        }

        public record LoginResponse(
                        String token,
                        String userId,
                        String email,
                        String roles,
                        String message) {
        }

        public record TokenRequest(String email, String password) {
        }

        public record TokenResponse(String token, String userId) {
        }

        public record ValidationRequest(String token) {
        }

        public record ValidationResponse(boolean valid, String message) {
        }

        public record UserInfoResponse(
                        String userId,
                        String email,
                        String firstName,
                        String lastName,
                        String roles,
                        String tenantId) {
        }
}
