package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotNull;

public record GradeSubmissionRequest(
        @NotNull(message = "Teacher ID is required") Integer teacherId,
        @NotNull(message = "Score is required") Integer score,
        String feedback) {
}
