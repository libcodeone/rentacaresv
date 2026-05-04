package com.rentacaresv.security.permission.application;

import com.rentacaresv.security.permission.domain.Permission;
import com.rentacaresv.security.permission.domain.PermissionEntity;
import com.rentacaresv.security.permission.domain.SystemRole;
import com.rentacaresv.security.permission.infrastructure.PermissionRepository;
import com.rentacaresv.security.permission.infrastructure.SystemRoleRepository;
import com.rentacaresv.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final SystemRoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    // ========================================
    // Operaciones de Roles
    // ========================================

    public List<SystemRole> getAllRoles() {
        return roleRepository.findAllOrdered();
    }

    public List<SystemRole> getActiveRoles() {
        return roleRepository.findAllActive();
    }

    public Optional<SystemRole> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public Optional<SystemRole> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    @Transactional
    public SystemRole createRole(String name, String displayName, String description, String color) {
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

    public List<Permission> getAllPermissions() {
        return Arrays.asList(Permission.values());
    }

    public Map<String, List<Permission>> getPermissionsByCategory() {
        return Arrays.stream(Permission.values())
                .collect(Collectors.groupingBy(Permission::getCategory,
                         LinkedHashMap::new,
                         Collectors.toList()));
    }

    @Transactional
    public void setRolePermissions(Long roleId, Set<Permission> permissions) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        Set<String> names = permissions.stream().map(Permission::name).collect(Collectors.toSet());
        Set<PermissionEntity> entities = new HashSet<>(permissionRepository.findAllByNameIn(names));

        role.setAllPermissions(entities);
        roleRepository.save(role);
        log.info("Permisos actualizados para rol {}: {} permisos", role.getDisplayName(), permissions.size());
    }

    @Transactional
    public void addPermissionToRole(Long roleId, Permission permission) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        PermissionEntity entity = permissionRepository.findByName(permission.name())
                .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado: " + permission.name()));

        role.addPermission(entity);
        roleRepository.save(role);
        log.info("Permiso {} agregado al rol {}", permission.name(), role.getDisplayName());
    }

    @Transactional
    public void removePermissionFromRole(Long roleId, Permission permission) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        permissionRepository.findByName(permission.name())
                .ifPresent(entity -> {
                    role.removePermission(entity);
                    roleRepository.save(role);
                    log.info("Permiso {} removido del rol {}", permission.name(), role.getDisplayName());
                });
    }

    // ========================================
    // Verificación de Permisos
    // ========================================

    public boolean userHasPermission(User user, Permission permission) {
        if (user == null) return false;
        if (user.isAdmin()) return true;

        for (var role : user.getRoles()) {
            Optional<SystemRole> systemRole = roleRepository.findByName(role.name());
            if (systemRole.isPresent() && systemRole.get().hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) return Collections.emptySet();
        if (user.isAdmin()) return new HashSet<>(Arrays.asList(Permission.values()));

        Set<Permission> permissions = new HashSet<>();
        for (var role : user.getRoles()) {
            Optional<SystemRole> systemRole = roleRepository.findByName(role.name());
            systemRole.ifPresent(sr -> permissions.addAll(sr.getPermissionEnums()));
        }
        return permissions;
    }

    // ========================================
    // Inicialización de Roles del Sistema
    // ========================================

    @Transactional
    public void initializeSystemRoles() {
        // 1. Sembrar la tabla permission desde el enum (idempotente)
        for (Permission p : Permission.values()) {
            if (!permissionRepository.existsByName(p.name())) {
                permissionRepository.save(PermissionEntity.from(p));
                log.debug("Permiso insertado: {}", p.name());
            }
        }

        // 2. Rol ADMIN — todos los permisos
        if (!roleRepository.existsByName("ADMIN")) {
            Set<PermissionEntity> allPerms = new HashSet<>(permissionRepository.findAll());
            SystemRole admin = SystemRole.builder()
                    .name("ADMIN")
                    .displayName("Administrador")
                    .description("Acceso completo al sistema")
                    .color("#DC2626")
                    .isSystemRole(true)
                    .active(true)
                    .permissions(allPerms)
                    .build();
            roleRepository.save(admin);
            log.info("Rol ADMIN inicializado con todos los permisos");
        }

        // 3. Rol OPERATOR — permisos operativos
        if (!roleRepository.existsByName("OPERATOR")) {
            Set<PermissionEntity> operatorPerms = findPermissionEntities(
                    Permission.VEHICLE_VIEW,
                    Permission.CUSTOMER_VIEW, Permission.CUSTOMER_CREATE, Permission.CUSTOMER_EDIT,
                    Permission.RENTAL_VIEW, Permission.RENTAL_CREATE, Permission.RENTAL_EDIT,
                    Permission.RENTAL_DELIVER, Permission.RENTAL_CANCEL,
                    Permission.CONTRACT_VIEW, Permission.CONTRACT_CREATE,
                    Permission.CONTRACT_SIGN, Permission.CONTRACT_DOWNLOAD,
                    Permission.PAYMENT_VIEW, Permission.PAYMENT_CREATE,
                    Permission.CALENDAR_VIEW,
                    Permission.CATALOG_VIEW
            );
            SystemRole operator = SystemRole.builder()
                    .name("OPERATOR")
                    .displayName("Operador")
                    .description("Acceso limitado para operaciones diarias")
                    .color("#2563EB")
                    .isSystemRole(true)
                    .active(true)
                    .permissions(operatorPerms)
                    .build();
            roleRepository.save(operator);
            log.info("Rol OPERATOR inicializado");
        }

        // 4. Rol AGENT — agente de entrega
        if (!roleRepository.existsByName("AGENT")) {
            Set<PermissionEntity> agentPerms = findPermissionEntities(
                    Permission.RENTAL_VIEW, Permission.RENTAL_DELIVER,
                    Permission.CONTRACT_VIEW, Permission.CONTRACT_SIGN, Permission.CONTRACT_DOWNLOAD
            );
            SystemRole agent = SystemRole.builder()
                    .name("AGENT")
                    .displayName("Agente de Entrega")
                    .description("Solo puede ver rentas, gestionar contratos y entregar vehículos")
                    .color("#059669")
                    .isSystemRole(true)
                    .active(true)
                    .permissions(agentPerms)
                    .build();
            roleRepository.save(agent);
            log.info("Rol AGENT inicializado");
        }
    }

    private Set<PermissionEntity> findPermissionEntities(Permission... permissions) {
        Set<String> names = Arrays.stream(permissions).map(Permission::name).collect(Collectors.toSet());
        return new HashSet<>(permissionRepository.findAllByNameIn(names));
    }
}
