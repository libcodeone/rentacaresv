package com.rentacaresv.security.passwordreset.infrastructure;

import com.rentacaresv.security.passwordreset.domain.PasswordResetToken;
import com.rentacaresv.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para tokens de recuperación de contraseña
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Busca un token por su valor
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Busca tokens activos de un usuario
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user = :user AND t.used = false AND t.expiresAt > :now")
    List<PasswordResetToken> findActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Busca tokens por usuario
     */
    List<PasswordResetToken> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Invalida todos los tokens activos de un usuario
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true, t.usedAt = :now WHERE t.user = :user AND t.used = false")
    int invalidateAllUserTokens(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Elimina tokens expirados (para limpieza)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :beforeDate")
    int deleteExpiredTokens(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Cuenta tokens creados por un usuario en las últimas N horas (para rate limiting)
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user.email = :email AND t.createdAt > :since")
    long countRecentTokensByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
}
