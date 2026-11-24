package com.example.learningplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Связь курса и тега")
public record CourseTagResponse(
        @Schema(description = "Идентификатор курса", example = "1") Integer courseId,
        @Schema(description = "Идентификатор тега", example = "2") Integer tagId) {
}
