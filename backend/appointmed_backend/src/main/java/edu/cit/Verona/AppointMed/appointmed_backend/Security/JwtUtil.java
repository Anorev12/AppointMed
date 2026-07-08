package edu.cit.Verona.AppointMed.appointmed_backend.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
 
import javax.crypto.SecretKey;
import java.util.Date;
 
/**
 * Issues and verifies tokens carrying the user's role and id.
 * This is what actually enforces "must not access another dashboard even
 * knowing the URL" — the frontend route guard calls /api/auth/me, which
 * uses isTokenValid() + extractRole() here to check the signature itself,
 * not just whatever the frontend claims.
 *
 * jwt.secret must be 32+ characters. Generate one with: openssl rand -base64 32
 */
@Component
public class JwtUtil {
 
    @Value("${jwt.secret}")
    private String secret;
 
    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;
 
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
 
    public String generateToken(Long id, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
 
        return Jwts.builder()
                .setSubject(email)
                .claim("id", id)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }
 
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
 
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
 
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }
 
    public Long extractId(String token) {
        return parseClaims(token).get("id", Long.class);
    }
 
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
 