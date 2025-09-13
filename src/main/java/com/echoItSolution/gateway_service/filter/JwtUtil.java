package com.echoItSolution.gateway_service.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component
public class JwtUtil {

    private SecretKey getSecretKey(){
        String secretKey = "gnr4ng458hjtg80459gh458g4580jg45rg";
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token); // will throw if invalid or expired
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Generic extractor using a resolver function. */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Will throw if token is tampered/invalid/expired (ExpiredJwtException extends JwtException)
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
