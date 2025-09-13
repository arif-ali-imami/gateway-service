package com.echoItSolution.gateway_service.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        log.info("Incoming request: [{}] {}", method, requestPath);

        // skip open endpoints
        if (requestPath.startsWith("/auth/login") || requestPath.startsWith("/public/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try{
            if (!jwtUtil.validateToken(token)) {
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }
        }catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return unauthorizedResponse(exchange, "JWT validation error");
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = String.format("{\"error\": \"%s\", \"status\": %d}",
                message, HttpStatus.UNAUTHORIZED.value());

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
