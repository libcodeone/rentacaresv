package com.rentacaresv.calendar.infrastructure;

import com.rentacaresv.calendar.domain.GoogleCalendarToken;
import com.rentacaresv.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para GoogleCalendarToken
 */
public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, Long> {

    /**
     * Busca el token por usuario
     */
    Optional<GoogleCalendarToken> findByUser(User user);

    /**
     * Busca el token por ID de usuario
     */
    Optional<GoogleCalendarToken> findByUserId(Long userId);

    /**
     * Verifica si un usuario tiene token
     */
    boolean existsByUserId(Long userId);

    /**
     * Elimina el token de un usuario
     */
    void deleteByUserId(Long userId);
}
