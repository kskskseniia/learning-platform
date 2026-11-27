package com.example.learningplatform.dto;

import java.util.List;

public record QuizResponse(
        Integer id,
        Integer moduleId,
        String title,
        Integer timeLimit,
        List<QuestionResponse> questions) {
}
