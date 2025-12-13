package com.fulus.ai.assistant.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for JWT token generation and validation
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${security.jwt.issuer}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // Ensure secret key is at least 256 bits (32 bytes)
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 characters)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtTokenProvider initialized with issuer: {}", issuer);
    }

    /**
     * Generate access token (JWT)
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("userId", userPrincipal.getId().toString())
                .claim("phoneNumber", userPrincipal.getPhoneNumber())
                .claim("name", userPrincipal.getName())
                .claim("authorities", authorities)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generate access token from user ID
     */
    public String generateAccessToken(UUID userId, String phoneNumber, String name) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("phoneNumber", phoneNumber)
                .claim("name", name)
                .claim("authorities", "ROLE_USER")
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Get user ID from JWT token
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    /**
     * Get phone number from JWT token
     */
    public String getPhoneNumberFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("phoneNumber", String.class);
    }

    /**
     * Get expiry date from token
     */
    public LocalDateTime getExpiryDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date expiration = claims.getExpiration();
        return Instant.ofEpochMilli(expiration.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            log.error("JWT signature validation failed: {}", ex.getMessage());
        }

        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get access token expiration in milliseconds
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get refresh token expiration in milliseconds
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
