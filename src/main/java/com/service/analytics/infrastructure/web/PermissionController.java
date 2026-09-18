package com.service.analytics.infrastructure.web;

import com.service.analytics.application.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST adapter exposing RBAC permission use cases backed by Redis.
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Void> grant(
            @PathVariable String userId,
            @RequestBody Map<String, Object> body) {
        permissionService.grant(userId, String.valueOf(body.get("permission")));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> revoke(
            @PathVariable String userId,
            @RequestBody Map<String, Object> body) {
        permissionService.revoke(userId, String.valueOf(body.get("permission")));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/{permission}")
    public ResponseEntity<Map<String, Boolean>> has(
            @PathVariable String userId,
            @PathVariable String permission) {
        boolean allowed = permissionService.hasPermission(userId, permission);
        return ResponseEntity.ok(Map.of("granted", allowed));
    }
}
