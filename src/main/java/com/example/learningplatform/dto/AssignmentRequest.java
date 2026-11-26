package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AssignmentRequest(
        @NotNull(message = "Teacher ID is required") Integer teacherId,
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,
        @NotBlank(message = "Description is required") String description,
        @NotNull(message = "Due date is required") LocalDate dueDate,
        @NotNull(message = "Max score is required")
        @Positive(message = "Max score must be positive")
        Integer maxScore) {
}
