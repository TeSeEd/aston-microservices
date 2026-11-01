package com.aston.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {
    @RequestMapping("/fallback/notifications")
    public ResponseEntity<Map<String,String>> notificationFallback(){
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message","Notification service temporarily unavailable"
                ));
    }
}
