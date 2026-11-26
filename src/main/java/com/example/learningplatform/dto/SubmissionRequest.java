package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmissionRequest(
        @NotNull(message = "Student ID is required") Integer studentId,
        @NotBlank(message = "Content is required") String content) {
}
