package com.munitax.tenant.controller;

import com.munitax.tenant.model.TaxReturnSession;
import com.munitax.tenant.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<TaxReturnSession> createSession(@RequestBody TaxReturnSession session) {
        TaxReturnSession created = sessionService.createSession(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<TaxReturnSession>> getAllSessions(
            @RequestParam String tenantId,
            @RequestParam String userId,
            @RequestParam(required = false) TaxReturnSession.SessionType type,
            @RequestParam(required = false) TaxReturnSession.SessionStatus status) {
        
        List<TaxReturnSession> sessions;
        
        if (type != null) {
            sessions = sessionService.getSessionsByType(tenantId, userId, type);
        } else if (status != null) {
            sessions = sessionService.getSessionsByStatus(tenantId, userId, status);
        } else {
            sessions = sessionService.getAllSessions(tenantId, userId);
        }
        
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<TaxReturnSession> getSession(@PathVariable String sessionId) {
        return sessionService.getSession(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<TaxReturnSession> updateSession(
            @PathVariable String sessionId,
            @RequestBody TaxReturnSession session) {
        try {
            TaxReturnSession updated = sessionService.updateSession(sessionId, session);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllSessions(
            @RequestParam String tenantId,
            @RequestParam String userId) {
        sessionService.deleteAllSessions(tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
