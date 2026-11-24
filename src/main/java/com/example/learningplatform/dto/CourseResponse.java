package com.example.learningplatform.dto;

import java.time.LocalDate;
import java.util.List;

public record CourseResponse(
        Integer id,
        String title,
        String description,
        Integer categoryId,
        Integer teacherId,
        String duration,
        LocalDate startDate,
        List<Integer> tagIds) {
}
