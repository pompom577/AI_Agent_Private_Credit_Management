package com.platform.gateway.repositories;

import com.platform.gateway.entities.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {}
