package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.domain.Contract;
import com.rentacaresv.contract.domain.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Contract
 */
public interface ContractRepository extends JpaRepository<Contract, Long> {

       /**
        * Busca un contrato por su token único (para acceso público)
        * Carga solo el contrato con rental, vehicle y customer (sin colecciones)
        */
       @Query("SELECT c FROM Contract c " +
                     "LEFT JOIN FETCH c.rental r " +
                     "LEFT JOIN FETCH r.vehicle " +
                     "LEFT JOIN FETCH r.customer " +
                     "WHERE c.token = :token")
       Optional<Contract> findByTokenBasic(@Param("token") String token);

       /**
        * Carga los accesorios de un contrato
        */
       @Query("SELECT c FROM Contract c " +
                     "LEFT JOIN FETCH c.accessories " +
                     "WHERE c.id = :id")
       Optional<Contract> findByIdWithAccessories(@Param("id") Long id);

       /**
        * Busca el contrato asociado a una renta
        */
       Optional<Contract> findByRentalId(Long rentalId);

       /**
        * Busca un contrato por ID básico (sin colecciones)
        * Incluye todos los campos del contrato incluyendo firmas
        */
       @Query("SELECT c FROM Contract c " +
                     "LEFT JOIN FETCH c.rental r " +
                     "LEFT JOIN FETCH r.vehicle v " +
                     "LEFT JOIN FETCH r.customer " +
                     "WHERE c.id = :id")
       Optional<Contract> findByIdBasic(@Param("id") Long id);

       /**
        * Verifica si existe un contrato para una renta
        */
       boolean existsByRentalId(Long rentalId);

       /**
        * Busca contratos por estado
        */
       List<Contract> findByStatus(ContractStatus status);

       /**
        * Busca contratos pendientes que han expirado
        */
       @Query("SELECT c FROM Contract c WHERE c.status = 'PENDING' AND c.expiresAt < :now")
       List<Contract> findExpiredContracts(@Param("now") LocalDateTime now);

       /**
        * Busca contratos firmados en un rango de fechas
        */
       @Query("SELECT c FROM Contract c WHERE c.status = 'SIGNED' AND c.signedAt BETWEEN :from AND :to")
       List<Contract> findSignedContractsBetween(
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

       /**
        * Cuenta contratos por estado
        */
       long countByStatus(ContractStatus status);
}
