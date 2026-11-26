package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModuleRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must contain at most 255 characters") String title,
        @NotNull(message = "Order index is required") Integer orderIndex,
        String description) {
}
