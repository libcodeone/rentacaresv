package com.rentacaresv.contract.infrastructure;

import com.rentacaresv.contract.domain.ContractAccessory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de ContractAccessory
 */
public interface ContractAccessoryRepository extends JpaRepository<ContractAccessory, Long> {

    /**
     * Busca los accesorios de un contrato
     */
    List<ContractAccessory> findByContractIdOrderByDisplayOrderAsc(Long contractId);

    /**
     * Elimina todos los accesorios de un contrato
     */
    void deleteByContractId(Long contractId);
}
