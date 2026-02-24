package com.itqgroup.service.dto;

import com.itqgroup.service.model.enums.DocumentAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryResponse {
    private Long id;
    private DocumentAction action;
    private String userId;
    private String comment;
    private LocalDateTime timestamp;
}