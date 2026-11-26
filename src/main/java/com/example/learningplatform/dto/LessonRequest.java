package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must contain at most 255 characters") String title,
        String content,
        @Size(max = 2048, message = "Video URL must contain at most 2048 characters") String videoUrl) {
}
