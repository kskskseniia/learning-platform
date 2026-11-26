package com.example.learningplatform.dto;

public record ModuleResponse(
        Integer id,
        Integer courseId,
        String title,
        Integer orderIndex,
        String description) {
}
