package com.rentacaresv.security.permission.application;

import com.rentacaresv.security.AuthenticatedUser;
import com.rentacaresv.security.permission.domain.Permission;
import com.rentacaresv.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Servicio para verificar permisos del usuario actual.
 * Diseñado para ser inyectado en vistas y componentes de UI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionChecker {

    private final AuthenticatedUser authenticatedUser;
    private final RolePermissionService rolePermissionService;

    /**
     * Verifica si el usuario actual tiene un permiso específico
     */
    public boolean hasPermission(Permission permission) {
        Optional<User> userOpt = authenticatedUser.get();
        if (userOpt.isEmpty()) {
            return false;
        }
        return rolePermissionService.userHasPermission(userOpt.get(), permission);
    }

    /**
     * Verifica si el usuario actual tiene alguno de los permisos especificados
     */
    public boolean hasAnyPermission(Permission... permissions) {
        Optional<User> userOpt = authenticatedUser.get();
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        for (Permission permission : permissions) {
            if (rolePermissionService.userHasPermission(user, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el usuario actual tiene todos los permisos especificados
     */
    public boolean hasAllPermissions(Permission... permissions) {
        Optional<User> userOpt = authenticatedUser.get();
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        for (Permission permission : permissions) {
            if (!rolePermissionService.userHasPermission(user, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Obtiene todos los permisos del usuario actual
     */
    public Set<Permission> getCurrentUserPermissions() {
        Optional<User> userOpt = authenticatedUser.get();
        if (userOpt.isEmpty()) {
            return Set.of();
        }
        return rolePermissionService.getUserPermissions(userOpt.get());
    }

    /**
     * Verifica si el usuario actual es administrador
     */
    public boolean isAdmin() {
        Optional<User> userOpt = authenticatedUser.get();
        return userOpt.isPresent() && userOpt.get().isAdmin();
    }

    /**
     * Obtiene el usuario actual
     */
    public Optional<User> getCurrentUser() {
        return authenticatedUser.get();
    }
}
