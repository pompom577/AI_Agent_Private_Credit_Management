package com.platform.gateway.repositories;

import com.platform.gateway.entities.AuditLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLedgerRepository extends JpaRepository<AuditLedger, Long> {
}
