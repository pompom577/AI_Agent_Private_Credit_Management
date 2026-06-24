package com.platform.gateway.repositories;

import com.platform.gateway.entities.DocumentCoordinate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoordinateRepository extends JpaRepository<DocumentCoordinate, Long> {
    Optional<DocumentCoordinate> findByMetricId(Long metricId);
}
