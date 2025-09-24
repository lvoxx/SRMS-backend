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

        @GetMapping("/services/{serviceName}")
        public ResponseEntity<?> serviceUnavailable(@PathVariable String serviceName) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(String
                                .format("Service [%s] is currently unavailable. Please try again later.", serviceName));
        }

        @GetMapping("/code/{code}")
        public ResponseEntity<?> handleError(@PathVariable String code) {
                HttpStatus status = HttpStatus.valueOf(code);
                return ResponseEntity.status(status.value()).body(status.getReasonPhrase());
        }
}
