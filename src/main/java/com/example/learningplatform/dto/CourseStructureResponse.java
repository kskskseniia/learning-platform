package com.example.learningplatform.dto;

import java.util.List;

public record CourseStructureResponse(
        Integer id, String title, List<ModuleStructureResponse> modules) {
}
