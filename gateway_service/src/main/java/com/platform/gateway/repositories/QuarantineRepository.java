package com.platform.gateway.repositories;

import com.platform.gateway.entities.QuarantinedPayload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuarantineRepository extends JpaRepository<QuarantinedPayload, UUID> {

    List<QuarantinedPayload> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
}