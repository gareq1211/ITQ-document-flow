package com.itqgroup.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitResult {
    private Long documentId;
    private Status status;
    private String message;

    public enum Status {
        SUCCESS,
        NOT_FOUND,
        CONFLICT,
        ERROR
    }

    public static SubmitResult success(Long documentId) {
        return SubmitResult.builder()
                .documentId(documentId)
                .status(Status.SUCCESS)
                .message("Document submitted successfully")
                .build();
    }

    public static SubmitResult notFound(Long documentId) {
        return SubmitResult.builder()
                .documentId(documentId)
                .status(Status.NOT_FOUND)
                .message("Document not found")
                .build();
    }

    public static SubmitResult conflict(Long documentId, String message) {
        return SubmitResult.builder()
                .documentId(documentId)
                .status(Status.CONFLICT)
                .message(message)
                .build();
    }

    public static SubmitResult error(Long documentId, String message) {
        return SubmitResult.builder()
                .documentId(documentId)
                .status(Status.ERROR)
                .message(message)
                .build();
    }
}