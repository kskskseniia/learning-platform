package com.example.learningplatform.dto;

import java.math.BigDecimal;

public record CourseAnalyticsResponse(
        Integer courseId,
        String courseTitle,
        long totalStudents,
        long activeStudents,
        long completedStudents,
        long assignments,
        long submissions,
        long gradedSubmissions,
        BigDecimal averageAssignmentScore,
        long quizzes,
        long quizAttempts,
        BigDecimal averageQuizScore,
        long reviews,
        BigDecimal averageReviewRating) {
}
