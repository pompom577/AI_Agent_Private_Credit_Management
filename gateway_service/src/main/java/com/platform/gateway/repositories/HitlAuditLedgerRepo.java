package com.platform.gateway.repositories;

import com.platform.gateway.entities.HitlAuditLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HitlAuditLedgerRepo extends JpaRepository<HitlAuditLedger, Long> {
}
