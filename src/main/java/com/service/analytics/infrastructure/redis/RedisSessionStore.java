package com.service.analytics.infrastructure.redis;

import com.service.analytics.domain.port.SessionStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Adapter implementing the SessionStore port against Redis.
 */
@Component
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;

    public RedisSessionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String sessionId, String data, long ttlSeconds) {
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, data, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Optional<String> get(String sessionId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + sessionId));
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}