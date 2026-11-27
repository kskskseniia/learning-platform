package com.example.learningplatform.dto;

import com.example.learningplatform.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionRequest(
        @NotBlank(message = "Question text is required") String text,
        @NotNull(message = "Question type is required") QuestionType type,
        @NotNull(message = "Answer options are required")
        @Size(min = 2, message = "Question must contain at least two answer options")
        List<@Valid AnswerOptionRequest> answerOptions) {
}
