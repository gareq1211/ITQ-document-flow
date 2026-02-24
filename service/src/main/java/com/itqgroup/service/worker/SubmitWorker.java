package com.itqgroup.service.worker;

import com.itqgroup.service.dto.DocumentActionRequest;
import com.itqgroup.service.dto.SubmitResult;
import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import com.itqgroup.service.repository.DocumentRepository;
import com.itqgroup.service.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitWorker {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @Value("${worker.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${worker.submit.interval:10000}")
    @Transactional
    public void processSubmits() {
        log.debug("Submit worker started. Looking for documents in DRAFT status (batch size: {})", batchSize);

        // Блокируем документы для обработки
        List<Document> documents = documentRepository.findAndLockByStatus(
                DocumentStatus.DRAFT,
                PageRequest.of(0, batchSize)
        );

        if (documents.isEmpty()) {
            log.debug("No documents to submit found");
            return;
        }

        log.info("Found {} documents to submit", documents.size());

        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(documentIds);
        request.setUserId("SYSTEM_WORKER");
        request.setComment("Auto-submitted by worker");

        long startTime = System.currentTimeMillis();

        List<SubmitResult> results = documentService.submitDocuments(request);

        long successCount = results.stream()
                .filter(r -> r.getStatus() == SubmitResult.Status.SUCCESS)
                .count();

        long failedCount = results.size() - successCount;

        long duration = System.currentTimeMillis() - startTime;

        log.info("Submit worker completed in {} ms. Success: {}, Failed: {}",
                duration, successCount, failedCount);
    }
}