package com.service.analytics.infrastructure.web;

import com.service.analytics.application.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * REST adapter exposing analyst-session use cases backed by Redis.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<Void> create(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        String data = String.valueOf(body.getOrDefault("data", ""));
        long ttl = ((Number) body.getOrDefault("ttlSeconds", 3600)).longValue();
        sessionService.createSession(sessionId, data, ttl);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<String> get(@PathVariable String sessionId) {
        Optional<String> data = sessionService.getSession(sessionId);
        return data.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable String sessionId) {
        sessionService.endSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
