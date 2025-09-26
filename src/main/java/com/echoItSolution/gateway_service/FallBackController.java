package com.echoItSolution.gateway_service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallBackController {

    @GetMapping("/fallback/booking")
    public ResponseEntity<String> fallBackMethod(){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Service is unavailable, please try again later");
    }
}
