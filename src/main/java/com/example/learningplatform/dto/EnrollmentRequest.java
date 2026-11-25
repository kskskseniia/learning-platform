package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
        @NotNull Integer studentId,
        @NotNull Integer courseId) {
}
