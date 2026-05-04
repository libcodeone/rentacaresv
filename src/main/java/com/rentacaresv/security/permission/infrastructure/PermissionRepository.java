package com.rentacaresv.security.permission.infrastructure;

import com.rentacaresv.security.permission.domain.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    boolean existsByName(String name);

    Optional<PermissionEntity> findByName(String name);

    List<PermissionEntity> findAllByNameIn(Collection<String> names);
}
