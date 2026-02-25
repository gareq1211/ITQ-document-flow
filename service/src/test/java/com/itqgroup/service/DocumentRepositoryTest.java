package com.itqgroup.service.repository;

import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void saveDocument_ShouldSetIdAndUniqueNumber() {
        // Given
        Document document = new Document();
        document.setAuthor("Test Author");
        document.setTitle("Test Title");

        // When
        Document saved = documentRepository.save(document);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUniqueNumber()).startsWith("DOC-");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_ShouldReturnDocument() {
        // Given
        Document document = new Document();
        document.setAuthor("Find Test");
        document.setTitle("Find Title");
        Document saved = documentRepository.save(document);

        // When
        Document found = documentRepository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByStatus_ShouldReturnDocumentsInStatus() {
        // Given
        Document draft1 = new Document();
        draft1.setAuthor("Author1");
        draft1.setTitle("Title1");
        documentRepository.save(draft1);

        Document draft2 = new Document();
        draft2.setAuthor("Author2");
        draft2.setTitle("Title2");
        documentRepository.save(draft2);

        Document submitted = new Document();
        submitted.setAuthor("Author3");
        submitted.setTitle("Title3");
        submitted.submit("user", "comment");
        documentRepository.save(submitted);

        // When
        List<Document> drafts = documentRepository.findAndLockByStatus(
                DocumentStatus.DRAFT,
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(drafts).hasSize(2);
        assertThat(drafts).allMatch(d -> d.getStatus() == DocumentStatus.DRAFT);
    }
}