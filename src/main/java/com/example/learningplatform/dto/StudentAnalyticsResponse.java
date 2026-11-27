package com.example.learningplatform.dto;

import java.math.BigDecimal;

public record StudentAnalyticsResponse(
        Integer studentId,
        String studentName,
        long enrolledCourses,
        long activeCourses,
        long completedCourses,
        long submittedAssignments,
        long gradedAssignments,
        BigDecimal averageAssignmentScore,
        long quizAttempts,
        BigDecimal averageQuizScore,
        Integer bestQuizScore) {
}
