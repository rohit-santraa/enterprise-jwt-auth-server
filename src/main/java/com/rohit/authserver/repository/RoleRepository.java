package com.rohit.authserver.repository;

import com.rohit.authserver.entity.ERole;
import com.rohit.authserver.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}