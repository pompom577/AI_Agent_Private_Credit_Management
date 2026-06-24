package com.platform.gateway.repositories;

import com.platform.gateway.entities.ExtractedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtractedMetricRepository extends JpaRepository<ExtractedMetric, Long> {
}
