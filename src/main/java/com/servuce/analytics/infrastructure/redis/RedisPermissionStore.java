package com.servuce.analytics.infrastructure.redis;

import com.servuce.analytics.domain.port.PermissionStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the PermissionStore port against Redis.
 * Permissions are stored in a Redis set per user.
 */
@Component
public class RedisPermissionStore implements PermissionStore {

    private static final String KEY_PREFIX = "permission:";

    private final StringRedisTemplate redisTemplate;

    public RedisPermissionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void grant(String userId, String permission) {
        redisTemplate.opsForSet().add(KEY_PREFIX + userId, permission);
    }

    @Override
    public void revoke(String userId, String permission) {
        redisTemplate.opsForSet().remove(KEY_PREFIX + userId, permission);
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(KEY_PREFIX + userId, permission));
    }
}