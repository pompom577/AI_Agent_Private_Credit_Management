package com.platform.gateway.repositories;

import com.platform.gateway.entities.QuarantinedPayload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// Person 2 adds findByStatus and other query methods for the compliance dashboard.
@Repository
public interface QuarantineRepository extends JpaRepository<QuarantinedPayload, UUID> {
}
