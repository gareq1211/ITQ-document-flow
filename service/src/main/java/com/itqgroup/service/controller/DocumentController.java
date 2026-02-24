package com.itqgroup.service.controller;

import com.itqgroup.service.dto.*;
import com.itqgroup.service.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentCreateRequest request) {
        log.info("REST request to create document: {}", request);
        DocumentResponse response = documentService.createDocument(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        log.info("REST request to get document with id: {}", id);
        DocumentResponse response = documentService.getDocumentWithHistory(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @RequestParam List<Long> ids,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST request to get documents with ids: {}, page: {}, size: {}", ids, page, size);
        List<DocumentResponse> responses = documentService.getDocumentsByIds(ids, page, size);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/submit")
    public ResponseEntity<List<SubmitResult>> submitDocuments(@Valid @RequestBody DocumentActionRequest request) {
        log.info("REST request to submit documents: {}", request);
        List<SubmitResult> results = documentService.submitDocuments(request);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/approve")
    public ResponseEntity<List<ApproveResult>> approveDocuments(@Valid @RequestBody DocumentActionRequest request) {
        log.info("REST request to approve documents: {}", request);
        List<ApproveResult> results = documentService.approveDocuments(request);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponse>> searchDocuments(@Valid DocumentSearchRequest request) {
        log.info("REST request to search documents with filters: {}", request);
        List<DocumentResponse> results = documentService.searchDocuments(request);
        return ResponseEntity.ok(results);
    }
}