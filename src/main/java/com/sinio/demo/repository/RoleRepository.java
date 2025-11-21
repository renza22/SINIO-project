package com.sinio.demo.repository;

import com.sinio.demo.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
    List<Role> findByCodeIn(Collection<String> codes);
}
