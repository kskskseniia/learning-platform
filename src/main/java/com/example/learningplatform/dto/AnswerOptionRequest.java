package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerOptionRequest(
        @NotBlank(message = "Answer option text is required") String text,
        @NotNull(message = "Correct flag is required") Boolean correct) {
}
