package com.linknest.linknest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;

@Component
public class JwtUtil {

    private static final byte[] SECRET_BYTES = new byte[64];
    static {
        new SecureRandom().nextBytes(SECRET_BYTES);
    }
    private static final Key SECRET_KEY = new SecretKeySpec(SECRET_BYTES, "HmacSHA512");
    private static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60 * 1000;

    public String generateToken(UserDetails userDetails) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(SECRET_KEY.getEncoded());
            return JWT.create()
                    .withSubject(userDetails.getUsername())
                    .withIssuedAt(new Date(System.currentTimeMillis()))
                    .withExpiresAt(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                    .sign(algorithm);
        } catch (Exception e) {
            System.err.println("Token generation failed: " + e.getMessage());
            return null;
        }
    }

    public String extractUsername(String token) {
        try {
            return JWT.require(Algorithm.HMAC512(SECRET_KEY.getEncoded()))
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (Exception e) {
            System.err.println("Token parsing failed: " + e.getMessage());
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = JWT.require(Algorithm.HMAC512(SECRET_KEY.getEncoded()))
                    .build()
                    .verify(token)
                    .getExpiresAt();
            return expiration.before(new Date());
        } catch (Exception e) {
            System.err.println("Expiration check failed: " + e.getMessage());
            return true;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        if (username == null) return false;
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}