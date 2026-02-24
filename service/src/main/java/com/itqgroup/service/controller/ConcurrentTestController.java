package com.itqgroup.service.controller;

import com.itqgroup.service.dto.ApproveResult;
import com.itqgroup.service.dto.ConcurrentTestRequest;
import com.itqgroup.service.dto.ConcurrentTestResult;
import com.itqgroup.service.dto.DocumentActionRequest;
import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import com.itqgroup.service.repository.DocumentRepository;
import com.itqgroup.service.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class ConcurrentTestController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    @PostMapping("/concurrent-approve/{documentId}")
    public ResponseEntity<ConcurrentTestResult> testConcurrentApprove(
            @PathVariable Long documentId,
            @Valid @RequestBody ConcurrentTestRequest request) {

        log.info("REST request to test concurrent approval for document {} with {} threads and {} attempts",
                documentId, request.getThreads(), request.getAttempts());

        // Проверяем существование документа
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        if (document.getStatus() != DocumentStatus.SUBMITTED) {
            throw new RuntimeException("Document must be in SUBMITTED status for test. Current status: " + document.getStatus());
        }

        ExecutorService executor = Executors.newFixedThreadPool(request.getThreads());
        List<Future<ApproveResult>> futures = new ArrayList<>();

        // Создаем запрос для утверждения
        DocumentActionRequest actionRequest = new DocumentActionRequest();
        actionRequest.setUserId("test-user");
        actionRequest.setComment("Concurrent test approval");

        // Запускаем параллельные попытки утверждения
        for (int i = 0; i < request.getAttempts(); i++) {
            futures.add(executor.submit(() -> {
                try {
                    // Каждая попытка утверждает ТОЛЬКО ЭТОТ ДОКУМЕНТ
                    List<Long> singleId = List.of(documentId);
                    actionRequest.setIds(singleId);

                    List<ApproveResult> results = documentService.approveDocuments(actionRequest);
                    return results.get(0);
                } catch (Exception e) {
                    log.error("Error in concurrent test attempt: {}", e.getMessage());
                    return ApproveResult.error(documentId, e.getMessage());
                }
            }));
        }

        executor.shutdown();

        // Собираем результаты
        int success = 0;
        int conflict = 0;
        int notFound = 0;
        int registryError = 0;
        int error = 0;

        for (Future<ApproveResult> future : futures) {
            try {
                ApproveResult result = future.get();
                switch (result.getStatus()) {
                    case SUCCESS:
                        success++;
                        break;
                    case CONFLICT:
                        conflict++;
                        break;
                    case NOT_FOUND:
                        notFound++;
                        break;
                    case REGISTRY_ERROR:
                        registryError++;
                        break;
                    default:
                        error++;
                }
            } catch (Exception e) {
                log.error("Failed to get result: {}", e.getMessage());
                error++;
            }
        }

        // Получаем финальный статус документа
        Document finalDocument = documentRepository.findById(documentId).orElse(null);
        DocumentStatus finalStatus = finalDocument != null ? finalDocument.getStatus() : null;

        ConcurrentTestResult result = ConcurrentTestResult.builder()
                .documentId(documentId)
                .successCount(success)
                .conflictCount(conflict)
                .notFoundCount(notFound)
                .registryErrorCount(registryError)
                .errorCount(error)
                .finalStatus(finalStatus)
                .build();

        log.info("Concurrent test completed: {}", result);

        return ResponseEntity.ok(result);
    }
}