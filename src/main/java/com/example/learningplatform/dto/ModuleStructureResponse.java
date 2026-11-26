package com.example.learningplatform.dto;

import java.util.List;

public record ModuleStructureResponse(
        Integer id,
        String title,
        Integer orderIndex,
        String description,
        List<LessonResponse> lessons) {
}
