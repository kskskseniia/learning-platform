package com.example.learningplatform.dto;

import java.time.LocalDate;

public record AssignmentResponse(
        Integer id,
        Integer lessonId,
        String title,
        String description,
        LocalDate dueDate,
        Integer maxScore) {
}
