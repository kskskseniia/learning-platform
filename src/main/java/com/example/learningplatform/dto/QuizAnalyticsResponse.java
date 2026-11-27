package com.example.learningplatform.dto;

import java.math.BigDecimal;

public record QuizAnalyticsResponse(
        Integer quizId,
        String quizTitle,
        long attempts,
        long uniqueStudents,
        BigDecimal averageScore,
        Integer bestScore,
        Integer worstScore) {
}
