package com.itqgroup.service.service;

import com.itqgroup.service.dto.*;
import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import com.itqgroup.service.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    private Document testDocument;

    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setAuthor("Test Author");
        testDocument.setTitle("Test Title");
        testDocument = documentRepository.save(testDocument);
    }

    @Test
    void createDocument_ShouldReturnDocumentResponse() {
        // Given
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setAuthor("New Author");
        request.setTitle("New Title");

        // When
        DocumentResponse response = documentService.createDocument(request);

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getUniqueNumber()).startsWith("DOC-");
        assertThat(response.getAuthor()).isEqualTo("New Author");
        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(response.getHistory()).isEmpty();
    }

    @Test
    void getDocumentWithHistory_ShouldReturnDocumentAndHistory() {
        // When
        DocumentResponse response = documentService.getDocumentWithHistory(testDocument.getId());

        // Then
        assertThat(response.getId()).isEqualTo(testDocument.getId());
        assertThat(response.getHistory()).isNotNull();
        assertThat(response.getHistory()).isEmpty();
    }

    @Test
    void submitDocuments_WithValidId_ShouldSucceed() {
        // Given
        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(List.of(testDocument.getId()));
        request.setUserId("test-user");
        request.setComment("Submitting for review");

        // When
        List<SubmitResult> results = documentService.submitDocuments(request);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(SubmitResult.Status.SUCCESS);

        Document updated = documentRepository.findById(testDocument.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
    }

    @Test
    void submitDocuments_WithInvalidId_ShouldReturnNotFound() {
        // Given
        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(List.of(999L));
        request.setUserId("test-user");

        // When
        List<SubmitResult> results = documentService.submitDocuments(request);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(SubmitResult.Status.NOT_FOUND);
    }

    @Test
    void submitDocuments_WithInvalidStatus_ShouldReturnConflict() {
        // Given
        testDocument.submit("user", "comment");
        documentRepository.save(testDocument);

        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(List.of(testDocument.getId()));
        request.setUserId("test-user");

        // When
        List<SubmitResult> results = documentService.submitDocuments(request);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(SubmitResult.Status.CONFLICT);
    }
}