package com.echoItSolution.gateway_service;

import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Configuration
public class AppConfig {


    // we can use this as a key resolver for rate limiting
    // on IP address either in yml or java config.
//    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange ->  Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }
    
//    @Bean
    public KeyResolver userKeyResolver(){
        return exchange -> {
            Principal principal = exchange.getPrincipal().block();
            return Mono.just(principal != null ? principal.getName() : "anonymous");
        };
    }

    // we can use this as a rate limiter config in gateway route filter.
//    @Bean
    public RedisRateLimiter redisRateLimiter(){
        return new RedisRateLimiter(5, 15, 1);
    }
}
