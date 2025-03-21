package com.gestioneFoto.repository;

import com.gestioneFoto.enums.RoleEnum;
import com.gestioneFoto.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleEnum name);
}
