package com.rentacaresv.customer.application;

import com.rentacaresv.customer.domain.Customer;
import com.rentacaresv.customer.domain.CustomerCategory;
import com.rentacaresv.customer.domain.DocumentType;
import com.rentacaresv.customer.infrastructure.CustomerMapper;
import com.rentacaresv.customer.infrastructure.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Servicio de aplicación para Customer
 * Orquesta los casos de uso relacionados con clientes
 */
@Service
@Transactional
@Validated
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    /**
     * Crea un nuevo cliente
     */
    public CustomerDTO createCustomer(@Valid CreateCustomerCommand command) {
        log.info("Creando cliente: {}", command.getFullName());

        // Validar que no exista el documento
        if (customerRepository.existsByDocumentNumber(command.getDocumentNumber())) {
            throw new IllegalArgumentException(
                "Ya existe un cliente con el documento: " + command.getDocumentNumber()
            );
        }

        // Crear entidad de dominio
        Customer customer = Customer.builder()
                .fullName(command.getFullName())
                .documentType(DocumentType.valueOf(command.getDocumentType().toUpperCase()))
                .documentNumber(command.getDocumentNumber())
                .email(command.getEmail())
                .phone(command.getPhone())
                .address(command.getAddress())
                .birthDate(command.getBirthDate())
                .driverLicenseNumber(command.getDriverLicenseNumber())
                .driverLicenseCountry(command.getDriverLicenseCountry())
                .driverLicenseExpiry(command.getDriverLicenseExpiry())
                .category(CustomerCategory.valueOf(command.getCategory().toUpperCase()))
                .notes(command.getNotes())
                .active(true)
                .build();

        // Persistir
        customer = customerRepository.save(customer);
        
        log.info("Cliente creado exitosamente: {}", customer.getDisplayName());
        
        return customerMapper.toDTO(customer);
    }

    /**
     * Obtiene un cliente por ID
     */
    @Transactional(readOnly = true)
    public CustomerDTO findById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + id));
        return customerMapper.toDTO(customer);
    }

    /**
     * Obtiene un cliente por número de documento
     */
    @Transactional(readOnly = true)
    public CustomerDTO findByDocumentNumber(String documentNumber) {
        Customer customer = customerRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con documento: " + documentNumber));
        return customerMapper.toDTO(customer);
    }

    /**
     * Obtiene todos los clientes activos
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> findAll() {
        List<Customer> customers = customerRepository.findAllActive();
        return customerMapper.toDTOList(customers);
    }

    /**
     * Obtiene clientes por categoría
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> findByCategory(CustomerCategory category) {
        List<Customer> customers = customerRepository.findByCategory(category);
        return customerMapper.toDTOList(customers);
    }

    /**
     * Obtiene clientes VIP
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> findVipCustomers() {
        List<Customer> customers = customerRepository.findActiveVipCustomers();
        return customerMapper.toDTOList(customers);
    }

    /**
     * Busca clientes por nombre
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> searchByName(String name) {
        List<Customer> customers = customerRepository.searchByName(name);
        return customerMapper.toDTOList(customers);
    }

    /**
     * Promociona un cliente a VIP
     */
    public void promoteToVip(Long customerId) {
        log.info("Promocionando cliente {} a VIP", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        
        customer.promoteToVip(); // Lógica de dominio
        customerRepository.save(customer);
    }

    /**
     * Cambia un cliente a categoría normal
     */
    public void demoteToNormal(Long customerId) {
        log.info("Cambiando cliente {} a categoría NORMAL", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        
        customer.demoteToNormal(); // Lógica de dominio
        customerRepository.save(customer);
    }

    /**
     * Actualiza información de contacto
     */
    public void updateContactInfo(Long customerId, String email, String phone, String address) {
        log.info("Actualizando información de contacto del cliente {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        
        customer.updateContactInfo(email, phone, address); // Lógica de dominio
        customerRepository.save(customer);
    }

    /**
     * Desactiva un cliente
     */
    public void deactivateCustomer(Long customerId) {
        log.info("Desactivando cliente {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        
        customer.deactivate(); // Lógica de dominio
        customerRepository.save(customer);
    }

    /**
     * Activa un cliente
     */
    public void activateCustomer(Long customerId) {
        log.info("Activando cliente {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        
        customer.activate(); // Lógica de dominio
        customerRepository.save(customer);
    }

    /**
     * Elimina un cliente (soft delete)
     */
    public void deleteCustomer(Long customerId) {
        log.info("Eliminando cliente {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        
        customer.delete(); // Lógica de dominio
        customerRepository.save(customer);
    }

    /**
     * Cuenta clientes VIP
     */
    @Transactional(readOnly = true)
    public long countVipCustomers() {
        return customerRepository.countVipCustomers();
    }

    /**
     * Cuenta clientes totales
     */
    @Transactional(readOnly = true)
    public long countAllCustomers() {
        return customerRepository.findAllActive().size();
    }
}
