package com.servuce.analytics.application.service;

import com.servuce.analytics.domain.port.PermissionStore;

/**
 * Application use case for role-based access control.
 */
public class PermissionService {

    private final PermissionStore permissionStore;

    public PermissionService(PermissionStore permissionStore) {
        this.permissionStore = permissionStore;
    }

    public void grant(String userId, String permission) {
        permissionStore.grant(userId, permission);
    }

    public void revoke(String userId, String permission) {
        permissionStore.revoke(userId, permission);
    }

    public boolean hasPermission(String userId, String permission) {
        return permissionStore.hasPermission(userId, permission);
    }
}