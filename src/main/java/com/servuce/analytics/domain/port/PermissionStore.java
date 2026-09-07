package com.servuce.analytics.domain.port;

/**
 * Port for storing and checking role-based access permissions (Redis in production).
 */
public interface PermissionStore {
    void grant(String userId, String permission);

    void revoke(String userId, String permission);

    boolean hasPermission(String userId, String permission);
}