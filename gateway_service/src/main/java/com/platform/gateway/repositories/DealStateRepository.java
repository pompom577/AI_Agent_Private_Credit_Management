package com.platform.gateway.repositories;

import com.platform.gateway.entities.DealState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@code deals} table (Story 1.2, Person 3).
 */
@Repository
public interface DealStateRepository extends JpaRepository<DealState, String> {
}
