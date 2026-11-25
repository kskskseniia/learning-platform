package com.example.learningplatform.dto;

import com.example.learningplatform.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record EnrollmentStatusRequest(@NotNull EnrollmentStatus status) {
}
