package com.itqgroup.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApproveResult {
    private Long documentId;
    private Status status;
    private String message;

    public enum Status {
        SUCCESS,
        NOT_FOUND,
        CONFLICT,
        REGISTRY_ERROR,
        ERROR
    }

    public static ApproveResult success(Long documentId) {
        return ApproveResult.builder()
                .documentId(documentId)
                .status(Status.SUCCESS)
                .message("Document approved successfully")
                .build();
    }

    public static ApproveResult notFound(Long documentId) {
        return ApproveResult.builder()
                .documentId(documentId)
                .status(Status.NOT_FOUND)
                .message("Document not found")
                .build();
    }

    public static ApproveResult conflict(Long documentId) {
        return ApproveResult.builder()
                .documentId(documentId)
                .status(Status.CONFLICT)
                .message("Document cannot be approved (invalid status)")
                .build();
    }

    public static ApproveResult registryError(Long documentId) {
        return ApproveResult.builder()
                .documentId(documentId)
                .status(Status.REGISTRY_ERROR)
                .message("Failed to create registry entry")
                .build();
    }

    public static ApproveResult error(Long documentId, String message) {
        return ApproveResult.builder()
                .documentId(documentId)
                .status(Status.ERROR)
                .message(message)
                .build();
    }
}