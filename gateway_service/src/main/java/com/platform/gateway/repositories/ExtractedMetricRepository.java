package com.platform.gateway.repositories;

import com.platform.gateway.entities.ExtractedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractedMetricRepository extends JpaRepository<ExtractedMetric, Long> {
    List<ExtractedMetric> findBySourceDocIdOrderByIdAsc(Long sourceDocId);
}
