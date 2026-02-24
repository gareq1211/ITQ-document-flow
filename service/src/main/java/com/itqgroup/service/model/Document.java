package com.itqgroup.service.model;

import com.itqgroup.service.model.enums.DocumentAction;
import com.itqgroup.service.model.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unique_number", nullable = false, unique = true)
    private String uniqueNumber;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocumentHistory> history = new ArrayList<>();

    @PrePersist
    public void generateUniqueNumber() {
        this.uniqueNumber = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void submit(String userId, String comment) {
        if (this.status != DocumentStatus.DRAFT) {
            throw new IllegalStateException("Document must be in DRAFT status to submit");
        }
        this.status = DocumentStatus.SUBMITTED;
        addHistory(DocumentAction.SUBMIT, userId, comment);
    }

    public void approve(String userId, String comment) {
        if (this.status != DocumentStatus.SUBMITTED) {
            throw new IllegalStateException("Document must be in SUBMITTED status to approve");
        }
        this.status = DocumentStatus.APPROVED;
        addHistory(DocumentAction.APPROVE, userId, comment);
    }

    private void addHistory(DocumentAction action, String userId, String comment) {
        DocumentHistory historyEntry = new DocumentHistory();
        historyEntry.setAction(action);
        historyEntry.setUserId(userId);
        historyEntry.setComment(comment);
        historyEntry.setTimestamp(LocalDateTime.now());
        historyEntry.setDocument(this);
        this.history.add(historyEntry);
    }
}