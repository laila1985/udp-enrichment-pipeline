package com.service.analytics.domain.port;

import java.util.Optional;

/**
 * Port for caching analyst sessions (Redis in production).
 */
public interface SessionStore {
    void save(String sessionId, String data, long ttlSeconds);

    Optional<String> get(String sessionId);

    void delete(String sessionId);
}