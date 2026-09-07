package com.servuce.analytics.application.service;

import com.servuce.analytics.domain.port.SessionStore;

import java.util.Optional;

/**
 * Application use case for managing analyst sessions.
 */
public class SessionService {

    private final SessionStore sessionStore;

    public SessionService(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public void createSession(String sessionId, String data, long ttlSeconds) {
        sessionStore.save(sessionId, data, ttlSeconds);
    }

    public Optional<String> getSession(String sessionId) {
        return sessionStore.get(sessionId);
    }

    public void endSession(String sessionId) {
        sessionStore.delete(sessionId);
    }
}