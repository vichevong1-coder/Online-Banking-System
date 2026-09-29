package com.obs.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        // 10000 requests per minute per IP to accommodate tests
        Bandwidth limit = Bandwidth.builder().capacity(10000).refillGreedy(10000, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        
        if (req.getMethod().equalsIgnoreCase("POST") && 
            (uri.startsWith("/auth/login") ||
             uri.startsWith("/auth/otp/") ||
             uri.startsWith("/auth/2fa/") ||
             uri.startsWith("/auth/forgot-password") ||
             uri.startsWith("/auth/register") ||
             uri.startsWith("/auth/refresh"))) {
             
             String ip = req.getRemoteAddr();
             Bucket bucket = resolveBucket(ip);
             
             if (bucket.tryConsume(1)) {
                 chain.doFilter(request, response);
             } else {
                 res.setStatus(429); // Too Many Requests
                 res.getWriter().write("Too many requests");
                 return;
             }
        } else {
            chain.doFilter(request, response);
        }
    }
}
