package com.itqgroup.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DocumentActionRequest {

    @NotEmpty(message = "Document IDs list cannot be empty")
    @Size(min = 1, max = 1000, message = "Document IDs list must contain between 1 and 1000 items")
    private List<Long> ids;

    @NotBlank(message = "User ID is required")
    private String userId;

    private String comment;
}