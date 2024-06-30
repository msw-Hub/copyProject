package io.cloudtype.Demo.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {
    private SecretKey accessSecretKey;
    private  SecretKey refreshSecretKey;

    public JWTUtil(@Value("${spring.jwt.accessSecretKey}")String access,
                   @Value("${spring.jwt.refreshSecretKey}")String refresh) {
        accessSecretKey = new SecretKeySpec(access.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        refreshSecretKey = new SecretKeySpec(refresh.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getUsername(String token, int key) {
        if(key==1) return Jwts.parser().verifyWith(accessSecretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
        else return Jwts.parser().verifyWith(refreshSecretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String getRole(String token,int key) {
        if(key==1) return Jwts.parser().verifyWith(accessSecretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
        else return Jwts.parser().verifyWith(refreshSecretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }
    public String getPassword(String token) {
        return Jwts.parser().verifyWith(accessSecretKey).build().parseSignedClaims(token).getPayload().get("password", String.class);
    }

    public Boolean isExpired(String token,int key) {
        try {
            if(key==1) {
                Jwts.parser().verifyWith(accessSecretKey).build().parseSignedClaims(token);
            } else {
                Jwts.parser().verifyWith(refreshSecretKey).build().parseSignedClaims(token);
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
        return false;
    }
    public String createAccessToken(String username, String role, Long expiredMs) {

        return Jwts.builder()
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(accessSecretKey)
                .compact();
    }
    public String createRefreshToken(String username, String role, Long expiredMs) {

        return Jwts.builder()
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(refreshSecretKey)
                .compact();
    }
}
