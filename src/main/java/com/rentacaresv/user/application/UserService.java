package com.rentacaresv.user.application;

import com.rentacaresv.security.Role;
import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Servicio para gestión de usuarios del sistema
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ========================================
    // Consultas
    // ========================================

    /**
     * Obtiene todos los usuarios
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Obtiene usuarios paginados
     */
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Obtiene usuarios con filtro de búsqueda
     */
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }

        String search = searchTerm.toLowerCase().trim();
        Specification<User> spec = (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), "%" + search + "%"),
                cb.like(cb.lower(root.get("name")), "%" + search + "%"),
                cb.like(cb.lower(root.get("email")), "%" + search + "%")
        );

        return userRepository.findAll(spec, pageable);
    }

    /**
     * Obtiene un usuario por ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Obtiene un usuario por username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Obtiene todos los usuarios activos
     */
    public List<User> getActiveUsers() {
        return userRepository.findAllActive();
    }

    // ========================================
    // Operaciones CRUD
    // ========================================

    /**
     * Crea un nuevo usuario
     */
    @Transactional
    public User createUser(String username, String fullName, String email, String password, Set<Role> roles) {
        // Validaciones
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario ya existe: " + username);
        }

        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya está registrado: " + email);
        }

        User user = User.builder()
                .username(username.toLowerCase().trim())
                .name(fullName.trim())
                .email(email != null ? email.toLowerCase().trim() : null)
                .hashedPassword(passwordEncoder.encode(password))
                .roles(roles != null && !roles.isEmpty() ? roles : Set.of(Role.OPERATOR))
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Usuario creado: {} ({})", fullName, username);
        return saved;
    }

    /**
     * Actualiza un usuario existente
     */
    @Transactional
    public User updateUser(Long userId, String fullName, String email, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        // Validar email único
        if (email != null && !email.isEmpty()) {
            Optional<User> existingEmail = userRepository.findByEmail(email.toLowerCase().trim());
            if (existingEmail.isPresent() && !existingEmail.get().getId().equals(userId)) {
                throw new IllegalArgumentException("El email ya está registrado por otro usuario");
            }
        }

        user.setName(fullName.trim());
        user.setEmail(email != null ? email.toLowerCase().trim() : null);
        
        if (roles != null && !roles.isEmpty()) {
            user.setRoles(new HashSet<>(roles));
        }

        User saved = userRepository.save(user);
        log.info("Usuario actualizado: {}", fullName);
        return saved;
    }

    /**
     * Cambia la contraseña de un usuario
     */
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Contraseña cambiada para usuario: {}", user.getUsername());
    }

    /**
     * Activa un usuario
     */
    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        user.activate();
        userRepository.save(user);
        log.info("Usuario activado: {}", user.getUsername());
    }

    /**
     * Desactiva un usuario
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        user.deactivate();
        userRepository.save(user);
        log.info("Usuario desactivado: {}", user.getUsername());
    }

    /**
     * Alterna el estado activo de un usuario
     */
    @Transactional
    public void toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        if (user.getActive()) {
            user.deactivate();
        } else {
            user.activate();
        }
        userRepository.save(user);
        log.info("Estado de usuario {} cambiado a: {}", user.getUsername(), user.getActive() ? "activo" : "inactivo");
    }

    /**
     * Elimina un usuario (solo si no es el último admin)
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        // Verificar que no sea el último admin
        if (user.isAdmin()) {
            long adminCount = userRepository.findAll().stream()
                    .filter(User::isAdmin)
                    .count();
            if (adminCount <= 1) {
                throw new IllegalStateException("No se puede eliminar el último administrador del sistema");
            }
        }

        userRepository.delete(user);
        log.info("Usuario eliminado: {}", user.getUsername());
    }

    // ========================================
    // Gestión de Roles
    // ========================================

    /**
     * Asigna roles a un usuario
     */
    @Transactional
    public void setUserRoles(Long userId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        // Verificar que no se quite el rol ADMIN al último admin
        if (user.isAdmin() && (roles == null || !roles.contains(Role.ADMIN))) {
            long adminCount = userRepository.findAll().stream()
                    .filter(User::isAdmin)
                    .count();
            if (adminCount <= 1) {
                throw new IllegalStateException("No se puede quitar el rol de administrador al último admin del sistema");
            }
        }

        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
        log.info("Roles actualizados para usuario {}: {}", user.getUsername(), roles);
    }

    /**
     * Agrega un rol a un usuario
     */
    @Transactional
    public void addRoleToUser(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(role);
        user.setRoles(roles);
        userRepository.save(user);
        log.info("Rol {} agregado al usuario {}", role, user.getUsername());
    }

    /**
     * Remueve un rol de un usuario
     */
    @Transactional
    public void removeRoleFromUser(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        // Verificar que no se quite el rol ADMIN al último admin
        if (role == Role.ADMIN && user.isAdmin()) {
            long adminCount = userRepository.findAll().stream()
                    .filter(User::isAdmin)
                    .count();
            if (adminCount <= 1) {
                throw new IllegalStateException("No se puede quitar el rol de administrador al último admin del sistema");
            }
        }

        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.remove(role);
        user.setRoles(roles);
        userRepository.save(user);
        log.info("Rol {} removido del usuario {}", role, user.getUsername());
    }

    // ========================================
    // Validaciones
    // ========================================

    /**
     * Verifica si un username está disponible
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username.toLowerCase().trim());
    }

    /**
     * Verifica si un email está disponible
     */
    public boolean isEmailAvailable(String email) {
        if (email == null || email.isEmpty()) {
            return true;
        }
        return !userRepository.existsByEmail(email.toLowerCase().trim());
    }

    /**
     * Verifica si un email está disponible para un usuario específico (excluyéndolo de la validación)
     */
    public boolean isEmailAvailable(String email, Long excludeUserId) {
        if (email == null || email.isEmpty()) {
            return true;
        }
        Optional<User> existing = userRepository.findByEmail(email.toLowerCase().trim());
        return existing.isEmpty() || existing.get().getId().equals(excludeUserId);
    }
}
