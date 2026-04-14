package com.api_gateway.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private static final List<String> PUBLIC_PATHS = List.of("/api/v0/auth", "/api/v0/auth/", "/test");

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith)
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            reject(response, HttpStatus.UNAUTHORIZED, "Missing or malformed Authorization token");
            return;
        }

        Claims claims;
        try {
            claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        } catch (SignatureException | IllegalArgumentException ex) {
            reject(response, HttpStatus.UNAUTHORIZED, "Invalid JWT token");
            return;
        }

        Date expiration = claims.getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            reject(response, HttpStatus.UNAUTHORIZED, "JWT token is expired");
            return;
        }

        String role = claims.get("role", String.class);
        if (isAdminPath(request.getRequestURI()) && !"ADMIN".equals(role)) {
            reject(response, HttpStatus.FORBIDDEN, "Insufficient privileges for admin route");
            return;
        }

        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
            request = new WrappedRequestWithHeader(request, HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/api/v0/admin/");
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> "jwt".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format("{\"error\":true,\"message\":\"%s\"}", message));
    }
}
