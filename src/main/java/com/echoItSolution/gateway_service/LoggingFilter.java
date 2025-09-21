package com.echoItSolution.gateway_service;

import io.jsonwebtoken.Claims;
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
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final GatewayConstant gatewayConstant;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        List<String> openPaths = gatewayConstant.getOpenPaths();
        log.info("Incoming request: [{}] {}", method, path);

        // Skip open endpoints
        if (openPaths.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        // skip open endpoints
//        if (path.startsWith("/auth/login") || path.startsWith("/public/") ||
//                path.startsWith("/api/v1/account/user/create")) {
//            return chain.filter(exchange);
//        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try{
            if (!jwtUtil.validateToken(token)) {
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }
            // Parse claims once and store for downstream services
            Claims claims = jwtUtil.parseToken(token);
            exchange.getAttributes().put("jwtClaims", claims);

        }catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return unauthorizedResponse(exchange, "JWT validation error");
        }
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            System.out.println("Post-filter: " + exchange.getRequest().getURI().getPath());
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Logging first, auth immediately after
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
