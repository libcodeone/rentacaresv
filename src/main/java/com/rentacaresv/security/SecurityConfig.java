package com.rentacaresv.security;

import com.rentacaresv.user.domain.User;
import com.rentacaresv.user.infrastructure.UserRepository;
import com.rentacaresv.views.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security para RentaCar ESV
 * 
 * - Autenticación basada en base de datos
 * - Integración con Vaadin Flow
 * - BCrypt para encriptación de contraseñas
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    /**
     * Configuración del encoder de contraseñas
     * BCrypt es el estándar actual para hash de passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración de seguridad HTTP
     * - Permite acceso público a recursos estáticos
     * - Configura login view y redirección
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Permitir acceso a recursos estáticos sin autenticación
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/images/**",           // Imágenes del proyecto
                    "/line-awesome/**",     // Iconos Line Awesome
                    "/themes/**",           // Temas de Vaadin
                    "/icons/**",            // Iconos adicionales
                    "/VAADIN/**",           // Recursos internos de Vaadin
                    "/favicon.ico",         // Favicon
                    "/manifest.webmanifest",// PWA manifest
                    "/sw.js",               // Service worker
                    "/offline.html"         // Página offline
                ).permitAll());

        // Configuración de Vaadin Security con login view
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer
                .loginView(LoginView.class));

        return http.build();
    }

    /**
     * Servicio de detalles de usuario
     * Carga usuarios desde la base de datos y los convierte a UserDetails de Spring Security
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            User user = userRepository.findByUsername(username);
            
            if (user == null) {
                throw new UsernameNotFoundException(
                    "Usuario no encontrado: " + username
                );
            }
            
            // Validar que el usuario esté activo
            if (!user.getActive()) {
                throw new UsernameNotFoundException(
                    "Usuario inactivo: " + username
                );
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getHashedPassword())
                    .roles(user.getRoles().stream()
                            .map(Role::name)
                            .toArray(String[]::new))
                    .build();
        };
    }
}
