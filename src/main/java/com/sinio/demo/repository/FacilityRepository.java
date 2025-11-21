package com.sinio.demo.repository;

import com.sinio.demo.model.Facility;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    Optional<Facility> findByNameIgnoreCase(String name);
}
