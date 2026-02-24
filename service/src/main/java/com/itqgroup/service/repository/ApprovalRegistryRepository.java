package com.itqgroup.service.repository;

import com.itqgroup.service.model.ApprovalRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {

    Optional<ApprovalRegistry> findByDocumentId(Long documentId);

    boolean existsByDocumentId(Long documentId);
}