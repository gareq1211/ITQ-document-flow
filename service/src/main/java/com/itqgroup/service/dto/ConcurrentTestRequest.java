package com.itqgroup.service.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ConcurrentTestRequest {

    @Min(value = 1, message = "Threads must be at least 1")
    private int threads = 5;

    @Min(value = 1, message = "Attempts must be at least 1")
    private int attempts = 10;
}