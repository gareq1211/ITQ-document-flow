package com.itqgroup.service.dto;

import com.itqgroup.service.model.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String uniqueNumber;
    private String author;
    private String title;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<HistoryResponse> history;
}