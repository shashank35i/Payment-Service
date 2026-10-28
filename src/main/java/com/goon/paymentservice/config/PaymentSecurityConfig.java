package com.goon.paymentservice.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PaymentSecurityConfig {
    @Bean
    SecurityFilterChain paymentSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/webhooks/razorpay").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payment-checkout.html").permitAll()
                        .requestMatchers("/payments/rides/**").access((authentication, context) -> {
                            var current = authentication.get();
                            boolean allowed = current instanceof JwtAuthenticationToken jwt
                                    && "goon-services".equals(jwt.getToken().getClaimAsString("azp"))
                                    && jwt.getAuthorities().stream()
                                    .anyMatch(a -> "SCOPE_payments.checkout".equals(a.getAuthority()));
                            return new AuthorizationDecision(allowed);
                        })
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}

