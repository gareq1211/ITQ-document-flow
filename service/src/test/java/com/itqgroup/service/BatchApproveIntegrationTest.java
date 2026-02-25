package com.itqgroup.service.integration;

import com.itqgroup.service.dto.ApproveResult;
import com.itqgroup.service.dto.DocumentActionRequest;
import com.itqgroup.service.model.ApprovalRegistry;
import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import com.itqgroup.service.repository.ApprovalRegistryRepository;
import com.itqgroup.service.repository.DocumentRepository;
import com.itqgroup.service.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BatchApproveIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApprovalRegistryRepository registryRepository;

    private List<Document> submittedDocuments;
    private List<Document> draftDocuments;

    @BeforeEach
    void setUp() {
        submittedDocuments = new ArrayList<>();
        draftDocuments = new ArrayList<>();

        // Создаем 5 документов в статусе SUBMITTED
        for (int i = 0; i < 5; i++) {
            Document doc = new Document();
            doc.setAuthor("Submit Author " + i);
            doc.setTitle("Submit Title " + i);
            doc.submit("user", "initial submit");
            doc = documentRepository.save(doc);
            submittedDocuments.add(doc);
        }

        // Создаем 3 документа в статусе DRAFT
        for (int i = 0; i < 3; i++) {
            Document doc = new Document();
            doc.setAuthor("Draft Author " + i);
            doc.setTitle("Draft Title " + i);
            doc = documentRepository.save(doc);
            draftDocuments.add(doc);
        }
    }

    @Test
    void approveDocuments_WithAllValid_ShouldSucceedForAll() {
        // Given
        List<Long> ids = submittedDocuments.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(ids);
        request.setUserId("approve-user");
        request.setComment("batch approve test");

        // When
        List<ApproveResult> results = documentService.approveDocuments(request);

        // Then
        assertThat(results).hasSize(5);
        assertThat(results).allMatch(r -> r.getStatus() == ApproveResult.Status.SUCCESS);

        // Проверяем статусы документов
        for (Document doc : submittedDocuments) {
            Document updated = documentRepository.findById(doc.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        }

        // Проверяем записи в реестре
        for (Document doc : submittedDocuments) {
            boolean registryExists = registryRepository.existsByDocumentId(doc.getId());
            assertThat(registryExists).isTrue();
        }
    }

    @Test
    void approveDocuments_WithMixedStatus_ShouldHandlePartialSuccess() {
        List<Long> ids = new ArrayList<>();
        ids.addAll(submittedDocuments.stream().map(Document::getId).collect(Collectors.toList()));
        ids.addAll(draftDocuments.stream().map(Document::getId).collect(Collectors.toList()));

        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(ids);
        request.setUserId("approve-user");

        List<ApproveResult> results = documentService.approveDocuments(request);

        assertThat(results).hasSize(8); // 5 + 3

        // Проверяем результаты для SUBMITTED документов
        long successCount = results.stream()
                .filter(r -> r.getStatus() == ApproveResult.Status.SUCCESS)
                .count();
        assertThat(successCount).isEqualTo(5);

        // Проверяем результаты для DRAFT документов
        long conflictCount = results.stream()
                .filter(r -> r.getStatus() == ApproveResult.Status.CONFLICT)
                .count();
        assertThat(conflictCount).isEqualTo(3);
    }

    @Test
    void approveDocuments_WithNonExistentIds_ShouldReturnNotFound() {
        List<Long> ids = List.of(999L, 1000L);
        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(ids);
        request.setUserId("approve-user");

        List<ApproveResult> results = documentService.approveDocuments(request);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getStatus() == ApproveResult.Status.NOT_FOUND);
    }

    @Test
    void approveDocuments_WithEmptyList_ShouldReturnEmpty() {
        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(List.of());
        request.setUserId("approve-user");

        List<ApproveResult> results = documentService.approveDocuments(request);

        assertThat(results).isEmpty();
    }
}