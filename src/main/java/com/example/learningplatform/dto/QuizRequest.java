package com.example.learningplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuizRequest(
        @NotNull(message = "Teacher ID is required") Integer teacherId,
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,
        @NotNull(message = "Time limit is required")
        @Positive(message = "Time limit must be positive")
        Integer timeLimit,
        @NotNull(message = "Questions are required")
        @Size(min = 1, message = "Quiz must contain at least one question")
        List<@Valid QuestionRequest> questions) {
}
