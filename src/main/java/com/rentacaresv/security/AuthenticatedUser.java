package com.rentacaresv.security;

import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Helper para manejar el usuario autenticado en la sesión actual
 * 
 * Proporciona métodos para:
 * - Obtener el usuario actual
 * - Cerrar sesión
 * - Actualizar último login
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUser {

    private final UserRepository userRepository;
    private final AuthenticationContext authenticationContext;

    /**
     * Obtiene el usuario actualmente autenticado
     * 
     * @return Optional con el usuario, o vacío si no hay sesión activa
     */
    public Optional<User> get() {
        return authenticationContext
                .getAuthenticatedUser(org.springframework.security.core.userdetails.User.class)
                .map(userDetails -> userRepository.findByUsername(userDetails.getUsername()));
    }

    /**
     * Cierra la sesión del usuario actual
     */
    public void logout() {
        authenticationContext.logout();
    }

    /**
     * Actualiza la fecha de último login del usuario actual
     */
    @Transactional
    public void updateLastLogin() {
        get().ifPresent(user -> {
            user.updateLastLogin();
            userRepository.save(user);
        });
    }
}
