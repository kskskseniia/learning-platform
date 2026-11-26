package com.example.learningplatform.dto;

public record LessonResponse(
        Integer id, Integer moduleId, String title, String content, String videoUrl) {
}
