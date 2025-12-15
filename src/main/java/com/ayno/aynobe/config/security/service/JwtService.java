package com.ayno.aynobe.config.security.service;

import com.ayno.aynobe.config.exception.CustomException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long ACCESS_EXP_MS  = 1000L * 60 * 15;         // 15분
    private static final long REFRESH_EXP_MS = 1000L * 60 * 60 * 24 * 7; // 7일

    // 캐싱
    private Key signKey;
    private JwtParser jwtParser;

    @PostConstruct
    void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.signKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parserBuilder().setSigningKey(this.signKey).build();
    }

    // 발급
    public String generateAccessToken(UserDetails principal) {
        return generateToken(principal, ACCESS_EXP_MS, "access");
    }

    public String generateRefreshToken(UserDetails principal) {
        return generateToken(principal, REFRESH_EXP_MS, "refresh");
    }

    private String generateToken(UserDetails subject, long expMillis, String tokenType) {
        long now = System.currentTimeMillis();
        // 권한을 문자열로 저장 (ROLE_USER, ROLE_ADMIN ...)
        List<String> roles = subject.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("token_type", tokenType);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expMillis))
                .signWith(signKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 파싱
    public record JwtPayload(String subject, List<String> roles, Date expiration, String tokenType) { }

    public JwtPayload payload(String token) throws JwtException {
        var body = jwtParser.parseClaimsJws(token).getBody();
        String subject = body.getSubject();
        Date exp = body.getExpiration();

        Object t = body.get("token_type");
        String tokenType = (t == null) ? null : String.valueOf(t);

        Object v = body.get("roles");
        List<String> roles = (v instanceof List<?> list)
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        return new JwtPayload(subject, roles, exp, tokenType);
    }

    public boolean isTokenValid(JwtPayload p, UserDetails user) {
        return user.getUsername().equals(p.subject()) && p.expiration().after(new Date());
    }

    public boolean isRefreshTokenValid(JwtPayload p) {
        return "refresh".equals(p.tokenType()) && p.expiration().after(new Date());
    }
}