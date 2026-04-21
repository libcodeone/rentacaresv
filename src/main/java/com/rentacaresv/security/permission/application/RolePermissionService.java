package com.rentacaresv.security.permission.application;

import com.rentacaresv.security.permission.domain.Permission;
import com.rentacaresv.security.permission.domain.SystemRole;
import com.rentacaresv.security.permission.infrastructure.SystemRoleRepository;
import com.rentacaresv.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de roles y permisos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final SystemRoleRepository roleRepository;

    // ========================================
    // Operaciones de Roles
    // ========================================

    /**
     * Obtiene todos los roles
     */
    public List<SystemRole> getAllRoles() {
        return roleRepository.findAllOrdered();
    }

    /**
     * Obtiene todos los roles activos
     */
    public List<SystemRole> getActiveRoles() {
        return roleRepository.findAllActive();
    }

    /**
     * Busca un rol por ID
     */
    public Optional<SystemRole> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    /**
     * Busca un rol por nombre
     */
    public Optional<SystemRole> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    /**
     * Crea un nuevo rol
     */
    @Transactional
    public SystemRole createRole(String name, String displayName, String description, String color) {
        // Validar que no exista
        if (roleRepository.existsByName(name.toUpperCase())) {
            throw new IllegalArgumentException("Ya existe un rol con el nombre: " + name);
        }

        SystemRole role = SystemRole.builder()
                .name(name.toUpperCase().replace(" ", "_"))
                .displayName(displayName)
                .description(description)
                .color(color)
                .isSystemRole(false)
                .active(true)
                .permissions(new HashSet<>())
                .build();

        SystemRole saved = roleRepository.save(role);
        log.info("Rol creado: {} ({})", displayName, name);
        return saved;
    }

    /**
     * Actualiza un rol existente
     */
    @Transactional
    public SystemRole updateRole(Long roleId, String displayName, String description, String color) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        role.setDisplayName(displayName);
        role.setDescription(description);
        role.setColor(color);

        SystemRole saved = roleRepository.save(role);
        log.info("Rol actualizado: {}", displayName);
        return saved;
    }

    /**
     * Elimina un rol (solo si no es de sistema)
     */
    @Transactional
    public void deleteRole(Long roleId) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        if (!role.canBeDeleted()) {
            throw new IllegalStateException("No se puede eliminar un rol del sistema");
        }

        roleRepository.delete(role);
        log.info("Rol eliminado: {}", role.getDisplayName());
    }

    /**
     * Activa/Desactiva un rol
     */
    @Transactional
    public void toggleRoleActive(Long roleId) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        role.setActive(!role.getActive());
        roleRepository.save(role);
        log.info("Rol {} {}", role.getDisplayName(), role.getActive() ? "activado" : "desactivado");
    }

    // ========================================
    // Operaciones de Permisos
    // ========================================

    /**
     * Obtiene todos los permisos disponibles
     */
    public List<Permission> getAllPermissions() {
        return Arrays.asList(Permission.values());
    }

    /**
     * Obtiene permisos agrupados por categoría
     */
    public Map<String, List<Permission>> getPermissionsByCategory() {
        return Arrays.stream(Permission.values())
                .collect(Collectors.groupingBy(Permission::getCategory, 
                         LinkedHashMap::new, // Mantener orden
                         Collectors.toList()));
    }

    /**
     * Asigna permisos a un rol
     */
    @Transactional
    public void setRolePermissions(Long roleId, Set<Permission> permissions) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        role.setAllPermissions(permissions);
        roleRepository.save(role);
        log.info("Permisos actualizados para rol {}: {} permisos", role.getDisplayName(), permissions.size());
    }

    /**
     * Agrega un permiso a un rol
     */
    @Transactional
    public void addPermissionToRole(Long roleId, Permission permission) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        role.addPermission(permission);
        roleRepository.save(role);
        log.info("Permiso {} agregado al rol {}", permission.name(), role.getDisplayName());
    }

    /**
     * Remueve un permiso de un rol
     */
    @Transactional
    public void removePermissionFromRole(Long roleId, Permission permission) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        role.removePermission(permission);
        roleRepository.save(role);
        log.info("Permiso {} removido del rol {}", permission.name(), role.getDisplayName());
    }

    // ========================================
    // Verificación de Permisos
    // ========================================

    /**
     * Verifica si un usuario tiene un permiso específico basado en sus roles
     */
    public boolean userHasPermission(User user, Permission permission) {
        if (user == null) {
            return false;
        }

        // Los usuarios con rol ADMIN tienen todos los permisos
        if (user.isAdmin()) {
            return true;
        }

        // Buscar en los roles del usuario
        for (var role : user.getRoles()) {
            Optional<SystemRole> systemRole = roleRepository.findByName(role.name());
            if (systemRole.isPresent() && systemRole.get().hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtiene todos los permisos de un usuario
     */
    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        // Los usuarios con rol ADMIN tienen todos los permisos
        if (user.isAdmin()) {
            return new HashSet<>(Arrays.asList(Permission.values()));
        }

        Set<Permission> permissions = new HashSet<>();
        for (var role : user.getRoles()) {
            Optional<SystemRole> systemRole = roleRepository.findByName(role.name());
            systemRole.ifPresent(sr -> permissions.addAll(sr.getPermissions()));
        }

        return permissions;
    }

    // ========================================
    // Inicialización de Roles del Sistema
    // ========================================

    /**
     * Inicializa los roles del sistema si no existen.
     * Se llama al iniciar la aplicación.
     */
    @Transactional
    public void initializeSystemRoles() {
        // Rol ADMIN - todos los permisos
        if (!roleRepository.existsByName("ADMIN")) {
            SystemRole admin = SystemRole.builder()
                    .name("ADMIN")
                    .displayName("Administrador")
                    .description("Acceso completo al sistema")
                    .color("#DC2626") // Rojo
                    .isSystemRole(true)
                    .active(true)
                    .permissions(new HashSet<>(Arrays.asList(Permission.values())))
                    .build();
            roleRepository.save(admin);
            log.info("Rol ADMIN inicializado con todos los permisos");
        }

        // Rol OPERATOR - permisos básicos
        if (!roleRepository.existsByName("OPERATOR")) {
            Set<Permission> operatorPermissions = new HashSet<>(Arrays.asList(
                    Permission.VEHICLE_VIEW,
                    Permission.CUSTOMER_VIEW,
                    Permission.CUSTOMER_CREATE,
                    Permission.CUSTOMER_EDIT,
                    Permission.RENTAL_VIEW,
                    Permission.RENTAL_CREATE,
                    Permission.RENTAL_EDIT,
                    Permission.CONTRACT_VIEW,
                    Permission.CONTRACT_CREATE,
                    Permission.CONTRACT_DOWNLOAD,
                    Permission.PAYMENT_VIEW,
                    Permission.PAYMENT_CREATE,
                    Permission.CALENDAR_VIEW,
                    Permission.CATALOG_VIEW
            ));

            SystemRole operator = SystemRole.builder()
                    .name("OPERATOR")
                    .displayName("Operador")
                    .description("Acceso limitado para operaciones diarias")
                    .color("#2563EB") // Azul
                    .isSystemRole(true)
                    .active(true)
                    .permissions(operatorPermissions)
                    .build();
            roleRepository.save(operator);
            log.info("Rol OPERATOR inicializado con permisos básicos");
        }
    }
}
