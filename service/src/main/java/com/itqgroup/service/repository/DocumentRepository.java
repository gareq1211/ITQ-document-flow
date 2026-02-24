package com.itqgroup.service.repository;

import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByUniqueNumber(String uniqueNumber);

    List<Document> findAllByIdIn(List<Long> ids, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.createdAt")
    List<Document> findAndLockByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    // Для поиска (задание 5)
    List<Document> findByStatusAndAuthorAndCreatedAtBetween(
            DocumentStatus status,
            String author,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    List<Document> findByStatusAndCreatedAtBetween(
            DocumentStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    List<Document> findByAuthorAndCreatedAtBetween(
            String author,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    List<Document> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);
}