package com.legal.contract.config;

import com.legal.contract.common.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/error"
    );

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        for (String publicUrl : PUBLIC_URLS) {
            if (requestUri.equals(publicUrl) || requestUri.endsWith(publicUrl)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未提供认证令牌\"}");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.validateToken(token);
            request.setAttribute("userId", Long.parseLong(claims.getSubject()));
            request.setAttribute("username", claims.get("username"));
            request.setAttribute("role", claims.get("role"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"令牌无效或已过期\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}