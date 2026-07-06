package com.example.demo.config;

import com.example.demo.account.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedOriginPatterns(List.of("*"));
                    corsConfiguration.setAllowedMethods(List.of("*"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**", "/login/**", "/oauth2/**").permitAll()
                    .requestMatchers("/api/products", "/api/products/**").permitAll()
                    .requestMatchers("/api/admin/crawler/**").permitAll()
                    .requestMatchers("/api/admin/system/public/**").permitAll()
                    .requestMatchers("/api/orders/track").permitAll()
                    .requestMatchers("/api/payment/**").permitAll()
                    .requestMatchers("/api/reviews/product/**").permitAll()
                    .requestMatchers("/api/v1/ai/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .oauth2Login(oauth -> oauth
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .defaultSuccessUrl("/api/auth/oauth2-success", true)
                    .failureHandler((request, response, exception) -> {
                        System.out.println("OAuth2 Login Failure: " + exception.getClass().getName() + " - " + exception.getMessage());
                        if (exception.getCause() != null) {
                            System.out.println("OAuth2 Failure Cause: " + exception.getCause().getMessage());
                        }
                        
                        String errMsg = exception.getMessage() != null ? exception.getMessage() : "";
                        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
                            errMsg += " " + exception.getCause().getMessage();
                        }
                        
                        if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException) {
                            org.springframework.security.oauth2.core.OAuth2Error oauth2Error = 
                                ((org.springframework.security.oauth2.core.OAuth2AuthenticationException) exception).getError();
                            if (oauth2Error != null) {
                                errMsg += " " + oauth2Error.getErrorCode() + " " + oauth2Error.getDescription();
                                System.out.println("OAuth2Error Details: Code=" + oauth2Error.getErrorCode() + ", Desc=" + oauth2Error.getDescription());
                            }
                        }

                        boolean isLocked = errMsg.toLowerCase().contains("khóa") 
                                        || errMsg.toLowerCase().contains("khoa") 
                                        || errMsg.toLowerCase().contains("lock");

                        String encodedError = java.net.URLEncoder.encode(
                            isLocked ? "Tài khoản của bạn đã bị khóa!" : "Đăng nhập thất bại!",
                            "UTF-8"
                        );
                        response.sendRedirect("http://localhost:5173/login?error=" + encodedError);
                    })
            )
            .exceptionHandling(exceptions -> exceptions
                    .defaultAuthenticationEntryPointFor(
                            (request, response, authException) -> {
                                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"message\": \"Vui lòng đăng nhập!\"}");
                            },
                            new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")
                    )
            );
        return http.build();
    }
}
