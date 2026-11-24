package com.example.learningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NameRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must contain at most 255 characters") String name) {
}
