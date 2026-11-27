package com.example.learningplatform.dto;

import java.time.LocalDateTime;

public record QuizSubmissionResponse(
        Integer id,
        Integer quizId,
        Integer studentId,
        Integer score,
        LocalDateTime takenAt) {
}
