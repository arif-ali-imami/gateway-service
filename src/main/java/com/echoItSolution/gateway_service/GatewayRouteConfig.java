package com.echoItSolution.gateway_service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class GatewayRouteConfig {

    private final AppConfig appConfig;

//    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-server", r -> r
                        .path("/auth/**")
                        .uri("lb://AUTH-SERVER"))
                .route("account-service", r -> r
                        .path("/api/v1/account/**", "/api/v1/outlet/**")
                        .filters(f ->
                                f.circuitBreaker(config ->
                                        config.setName("gatewayLevelCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/booking"))
                                .requestRateLimiter(config -> config.setRateLimiter(appConfig.redisRateLimiter())
                                        .setKeyResolver(appConfig.ipKeyResolver())
                                )
                        )
                        .uri("lb://ACCOUNT-SERVICE")
                )
                .route("booking-service", r -> r
                        .path("/api/booking/**")
                        .filters(f ->
                                f.circuitBreaker(config ->
                                        config.setName("gatewayLevelCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/booking"))
                                .rewritePath("/api/booking/(?<segment>.*)", "/booking/${segment}")
                        )
                        .uri("lb://BOOKING-SERVICE"))
                .route("payment-service", r -> r
                        .path("/payment/**")
                        .uri("lb://PAYMENT-SERVICE"))
                .build();
    }
}
