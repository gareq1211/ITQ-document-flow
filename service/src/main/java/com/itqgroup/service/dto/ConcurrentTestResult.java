package com.itqgroup.service.dto;

import com.itqgroup.service.model.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConcurrentTestResult {
    private int successCount;
    private int conflictCount;
    private int notFoundCount;
    private int registryErrorCount;
    private int errorCount;
    private DocumentStatus finalStatus;
    private Long documentId;
}