package com.example.learningplatform.dto;

import java.time.LocalDateTime;

public record CourseReviewResponse(
        Integer id,
        Integer courseId,
        Integer studentId,
        Integer rating,
        String comment,
        LocalDateTime createdAt) {
}
