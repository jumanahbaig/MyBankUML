package server;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "MyBankUML_Secret_Key_For_JWT_Tokens_Must_Be_At_Least_256_Bits_Long";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public static String generateToken(String username, String role, long userId) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY)
                .compact();
    }

    public static Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public static String getRoleFromToken(String token) {
        return validateToken(token).get("role", String.class);
    }

    public static Long getUserIdFromToken(String token) {
        return validateToken(token).get("userId", Long.class);
    }
}
