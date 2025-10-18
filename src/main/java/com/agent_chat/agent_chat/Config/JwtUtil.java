package com.agent_chat.agent_chat.Config;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

  // Secret key - nên lưu trong environment variable
  private static final String SECRET_KEY = "MySecretKeyForJWTTokenGenerationAndValidation12345678901234567890";

  // Access token validity: 15 minutes
  private static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000;

  // Refresh token validity: 7 days
  private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000;

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
  }

  // Generate Access Token
  public String generateAccessToken(String email, String userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "access");
    claims.put("userId", userId);
    return createToken(claims, email, ACCESS_TOKEN_VALIDITY);
  }

  // Generate Refresh Token
  public String generateRefreshToken(String email, String userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    claims.put("userId", userId);
    return createToken(claims, email, REFRESH_TOKEN_VALIDITY);
  }

  private String createToken(Map<String, Object> claims, String subject, long validity) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + validity))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  // Extract email from token
  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  // Extract userId from token
  public String extractUserId(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("userId", String.class);
  }

  // Extract expiration date
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  // Check if token is expired
  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  // Validate token
  public Boolean validateToken(String token, String email) {
    final String extractedEmail = extractEmail(token);
    return (extractedEmail.equals(email) && !isTokenExpired(token));
  }

  // Get token type (access or refresh)
  public String getTokenType(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("type", String.class);
  }
}
