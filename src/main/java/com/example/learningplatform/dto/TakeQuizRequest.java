package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record TakeQuizRequest(
        @NotNull(message = "Student ID is required") Integer studentId,
        @NotNull(message = "Selected answers are required")
        Map<Integer, List<Integer>> selectedAnswers) {
}
