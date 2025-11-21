package com.sinio.demo.repository;

import com.sinio.demo.model.Karyawan;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KaryawanRepository extends JpaRepository<Karyawan, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<Karyawan> findByUserId(Long userId);
}
