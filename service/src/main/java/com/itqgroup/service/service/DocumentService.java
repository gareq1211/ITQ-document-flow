package com.itqgroup.service.service;

import com.itqgroup.service.dto.*;
import com.itqgroup.service.model.ApprovalRegistry;
import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import com.itqgroup.service.repository.ApprovalRegistryRepository;
import com.itqgroup.service.repository.DocumentRepository;
import com.itqgroup.service.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final HistoryRepository historyRepository;
    private final ApprovalRegistryRepository registryRepository;

    @Transactional
    public DocumentResponse createDocument(DocumentCreateRequest request) {
        log.debug("Creating new document with author: {}, title: {}", request.getAuthor(), request.getTitle());

        Document document = new Document();
        document.setAuthor(request.getAuthor());
        document.setTitle(request.getTitle());

        Document savedDocument = documentRepository.save(document);
        log.info("Document created successfully with id: {}, number: {}", savedDocument.getId(), savedDocument.getUniqueNumber());

        return mapToResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentWithHistory(Long id) {
        log.debug("Fetching document with id: {} and its history", id);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByIds(List<Long> ids, int page, int size) {
        log.debug("Fetching documents by ids: {}, page: {}, size: {}", ids, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        List<Document> documents = documentRepository.findAllByIdIn(ids, pageable);

        return documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<SubmitResult> submitDocuments(DocumentActionRequest request) {
        log.info("Processing submit for {} documents by user: {}", request.getIds().size(), request.getUserId());

        List<SubmitResult> results = new ArrayList<>();

        for (Long documentId : request.getIds()) {
            try {
                Document document = documentRepository.findById(documentId)
                        .orElseThrow(() -> new RuntimeException("Document not found"));

                document.submit(request.getUserId(), request.getComment());
                documentRepository.save(document);

                log.debug("Document {} submitted successfully", documentId);
                results.add(SubmitResult.success(documentId));

            } catch (RuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    log.warn("Document {} not found", documentId);
                    results.add(SubmitResult.notFound(documentId));
                } else if (e instanceof IllegalStateException) {
                    log.warn("Document {} cannot be submitted: {}", documentId, e.getMessage());
                    results.add(SubmitResult.conflict(documentId, e.getMessage()));
                } else {
                    log.error("Error submitting document {}: {}", documentId, e.getMessage());
                    results.add(SubmitResult.error(documentId, e.getMessage()));
                }
            }
        }

        log.info("Submit completed. Success: {}, Failed: {}",
                results.stream().filter(r -> r.getStatus() == SubmitResult.Status.SUCCESS).count(),
                results.size() - results.stream().filter(r -> r.getStatus() == SubmitResult.Status.SUCCESS).count());

        return results;
    }

    @Transactional
    public List<ApproveResult> approveDocuments(DocumentActionRequest request) {
        log.info("Processing approve for {} documents by user: {}", request.getIds().size(), request.getUserId());

        List<ApproveResult> results = new ArrayList<>();

        for (Long documentId : request.getIds()) {
            try {
                Document document = documentRepository.findById(documentId)
                        .orElseThrow(() -> new RuntimeException("Document not found"));

                document.approve(request.getUserId(), request.getComment());
                documentRepository.save(document);

                // Создаем запись в реестре утверждений
                try {
                    ApprovalRegistry registry = new ApprovalRegistry();
                    registry.setDocumentId(documentId);
                    registry.setApprovedAt(LocalDateTime.now());
                    registry.setApprovedBy(request.getUserId());

                    registryRepository.save(registry);
                    log.debug("Registry entry created for document {}", documentId);

                } catch (Exception e) {
                    log.error("Failed to create registry entry for document {}: {}", documentId, e.getMessage());
                    throw new RuntimeException("Failed to create registry entry", e);
                }

                log.debug("Document {} approved successfully", documentId);
                results.add(ApproveResult.success(documentId));

            } catch (RuntimeException e) {
                if (e.getMessage().contains("not found")) {
                    log.warn("Document {} not found", documentId);
                    results.add(ApproveResult.notFound(documentId));
                } else if (e instanceof IllegalStateException) {
                    log.warn("Document {} cannot be approved: {}", documentId, e.getMessage());
                    results.add(ApproveResult.conflict(documentId));
                } else if (e.getMessage().contains("registry")) {
                    log.error("Registry error for document {}: {}", documentId, e.getMessage());
                    results.add(ApproveResult.registryError(documentId));
                } else {
                    log.error("Error approving document {}: {}", documentId, e.getMessage());
                    results.add(ApproveResult.error(documentId, e.getMessage()));
                }
            }
        }

        log.info("Approve completed. Success: {}, Failed: {}",
                results.stream().filter(r -> r.getStatus() == ApproveResult.Status.SUCCESS).count(),
                results.size() - results.stream().filter(r -> r.getStatus() == ApproveResult.Status.SUCCESS).count());

        return results;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> searchDocuments(DocumentSearchRequest request) {
        log.debug("Searching documents with filters: status={}, author={}, dateFrom={}, dateTo={}",
                request.getStatus(), request.getAuthor(), request.getDateFrom(), request.getDateTo());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("createdAt").descending());
        List<Document> documents;

        // Поиск по комбинации фильтров (используем дату создания)
        if (request.getStatus() != null && request.getAuthor() != null && request.getDateFrom() != null && request.getDateTo() != null) {
            documents = documentRepository.findByStatusAndAuthorAndCreatedAtBetween(
                    request.getStatus(), request.getAuthor(), request.getDateFrom(), request.getDateTo(), pageable);
        } else if (request.getStatus() != null && request.getAuthor() != null) {
            documents = documentRepository.findByStatusAndAuthorAndCreatedAtBetween(
                    request.getStatus(), request.getAuthor(),
                    request.getDateFrom() != null ? request.getDateFrom() : LocalDateTime.MIN,
                    request.getDateTo() != null ? request.getDateTo() : LocalDateTime.MAX,
                    pageable);
        } else if (request.getStatus() != null) {
            documents = documentRepository.findByStatusAndCreatedAtBetween(
                    request.getStatus(),
                    request.getDateFrom() != null ? request.getDateFrom() : LocalDateTime.MIN,
                    request.getDateTo() != null ? request.getDateTo() : LocalDateTime.MAX,
                    pageable);
        } else if (request.getAuthor() != null) {
            documents = documentRepository.findByAuthorAndCreatedAtBetween(
                    request.getAuthor(),
                    request.getDateFrom() != null ? request.getDateFrom() : LocalDateTime.MIN,
                    request.getDateTo() != null ? request.getDateTo() : LocalDateTime.MAX,
                    pageable);
        } else if (request.getDateFrom() != null && request.getDateTo() != null) {
            documents = documentRepository.findByCreatedAtBetween(request.getDateFrom(), request.getDateTo(), pageable);
        } else {
            documents = documentRepository.findAll(pageable).getContent();
        }

        return documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .uniqueNumber(document.getUniqueNumber())
                .author(document.getAuthor())
                .title(document.getTitle())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .history(document.getHistory().stream()
                        .map(h -> HistoryResponse.builder()
                                .id(h.getId())
                                .action(h.getAction())
                                .userId(h.getUserId())
                                .comment(h.getComment())
                                .timestamp(h.getTimestamp())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}