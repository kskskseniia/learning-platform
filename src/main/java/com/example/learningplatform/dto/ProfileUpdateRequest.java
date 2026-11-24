package com.example.learningplatform.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        String bio,
        @Size(max = 2048, message = "Avatar URL must contain at most 2048 characters")
        String avatarUrl) {
}
