package com.example.learningplatform.dto;

import java.time.LocalDateTime;

public record SubmissionResponse(
        Integer id,
        Integer assignmentId,
        Integer studentId,
        LocalDateTime submittedAt,
        String content,
        Integer score,
        String feedback) {
}
