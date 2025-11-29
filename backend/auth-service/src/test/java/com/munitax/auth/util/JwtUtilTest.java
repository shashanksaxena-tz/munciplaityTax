package com.munitax.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private Key jwtKey;
    private long jwtExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        String secret = "defaultSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm";
        jwtKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Test
    void testGenerateToken() {
        String email = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", "ROLE_INDIVIDUAL");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtKey)
                .compact();

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testExtractUsername() {
        String email = "test@example.com";
        String token = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtKey)
                .compact();

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(email, claims.getSubject());
    }

    @Test
    void testTokenExpiration() {
        String email = "test@example.com";
        String token = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago (expired)
                .signWith(jwtKey)
                .compact();

        assertThrows(Exception.class, () -> {
            Jwts.parserBuilder()
                    .setSigningKey(jwtKey)
                    .build()
                    .parseClaimsJws(token);
        });
    }

    @Test
    void testTokenWithClaims() {
        String email = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", "ROLE_INDIVIDUAL,ROLE_BUSINESS");
        claims.put("userId", "123");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtKey)
                .compact();

        Claims extractedClaims = Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("ROLE_INDIVIDUAL,ROLE_BUSINESS", extractedClaims.get("roles"));
        assertEquals("123", extractedClaims.get("userId"));
    }

    @Test
    void testInvalidSignature() {
        String email = "test@example.com";
        String token = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtKey)
                .compact();

        // Try to validate with different key
        Key wrongKey = Keys.hmacShaKeyFor("differentSecretKeyThatIsAlsoAtLeast256BitsLong!".getBytes());

        assertThrows(Exception.class, () -> {
            Jwts.parserBuilder()
                    .setSigningKey(wrongKey)
                    .build()
                    .parseClaimsJws(token);
        });
    }
}
