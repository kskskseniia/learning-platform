package com.example.learningplatform.dto;

import com.example.learningplatform.entity.EnrollmentStatus;
import java.time.LocalDate;

public record EnrollmentResponse(
        Integer id,
        Integer studentId,
        Integer courseId,
        LocalDate enrollDate,
        EnrollmentStatus status) {
}
