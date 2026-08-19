package com.obs.backend.security;

import com.obs.backend.security.jwt.JwtAuthenticationFilter;
import com.obs.backend.security.jwt.JwtService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Local demo only: the web-admin dev server (US-002, Vite default port) runs on a different
    // origin than the backend, so the browser needs an explicit CORS allow rather than the
    // same-origin default. Override via WEB_ADMIN_ORIGIN for anything beyond a local run.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${web-admin.origin:http://localhost:5173}") String webAdminOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(webAdminOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register",
                                "/auth/otp/verify",
                                "/auth/otp/resend",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/2fa/verify",
                                "/auth/2fa/resend")
                        .permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        // Spring forwards here to render error bodies (e.g. @Valid failures as
                        // 400s). Without this, that internal forward hits anyRequest().authenticated()
                        // and every validation error comes back as an opaque 403 instead.
                        .requestMatchers("/error")
                        .permitAll()
                        .anyRequest().authenticated())
                // Without an entry point, an unauthenticated request falls through as anonymous and
                // Spring answers 403, which is indistinguishable from "signed in but not allowed".
                // The web-admin client needs to tell those apart: a 401 means the access token
                // expired and is worth refreshing and retrying, whereas a 403 never is. Role
                // denials keep the default 403 via the access-denied handler.
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"UNAUTHENTICATED\"}");
                        }))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
