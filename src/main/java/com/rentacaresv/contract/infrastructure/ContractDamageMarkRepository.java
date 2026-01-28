package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.domain.ContractDamageMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de ContractDamageMark
 */
public interface ContractDamageMarkRepository extends JpaRepository<ContractDamageMark, Long> {

    /**
     * Busca las marcas de daño de un contrato
     */
    List<ContractDamageMark> findByContractId(Long contractId);

    /**
     * Busca solo los daños preexistentes
     */
    List<ContractDamageMark> findByContractIdAndIsPreExistingTrue(Long contractId);

    /**
     * Busca solo los daños nuevos
     */
    List<ContractDamageMark> findByContractIdAndIsPreExistingFalse(Long contractId);

    /**
     * Elimina todas las marcas de un contrato
     */
    void deleteByContractId(Long contractId);

    /**
     * Cuenta daños por contrato
     */
    long countByContractId(Long contractId);
}
