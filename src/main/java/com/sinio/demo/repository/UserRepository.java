package com.sinio.demo.repository;

import com.sinio.demo.model.User;
import com.sinio.demo.model.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    List<User> findAllByRoleOrderByFullNameAsc(UserRole role);
}
