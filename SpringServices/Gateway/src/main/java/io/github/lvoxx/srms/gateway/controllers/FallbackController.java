package io.github.lvoxx.srms.gateway.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

        @GetMapping("/{serviceName}")
        public ResponseEntity<String> serviceUnavailable(@PathVariable String serviceName) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(String
                                .format("Service %s is currently unavailable. Please try again later.", serviceName));
        }

        @GetMapping("/{code}")
        public ResponseEntity<String> handleError(@PathVariable String code) {
                return switch (code) {
                        case "403" -> ResponseEntity.status(403).body("Access Denied");
                        case "429" -> ResponseEntity.status(429).body("Rate Limit Exceeded");
                        case "503" -> ResponseEntity.status(503).body("Service Unavailable");
                        case "404" -> ResponseEntity.status(404).body("Route Not Found");
                        default -> ResponseEntity.status(500).body("Internal Server Error");
                };
        }
}
