package com.example.learningplatform.dto;

import java.math.BigDecimal;

public record LeaderboardEntryResponse(
        long rank,
        Integer studentId,
        String studentName,
        BigDecimal assignmentAverage,
        BigDecimal quizAverage,
        BigDecimal overallScore,
        long completedActivities) {
}
