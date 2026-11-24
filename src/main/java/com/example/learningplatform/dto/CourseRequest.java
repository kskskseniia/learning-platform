package com.example.learningplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record CourseRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must contain at most 255 characters") String title,
        @NotBlank(message = "Description is required") String description,
        @NotNull(message = "Category is required") Integer categoryId,
        @NotNull(message = "Teacher is required") Integer teacherId,
        @NotBlank(message = "Duration is required")
        @Size(max = 255, message = "Duration must contain at most 255 characters") String duration,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "Tags are required") Set<@Valid @NotNull Integer> tagIds) {
}
