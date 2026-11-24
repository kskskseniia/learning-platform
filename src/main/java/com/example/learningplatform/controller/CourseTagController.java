package com.example.learningplatform.controller;

import com.example.learningplatform.dto.CourseTagResponse;
import com.example.learningplatform.service.CourseTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-tags")
@Tag(name = "Course Tags", description = "Просмотр связей курсов и тегов")
public class CourseTagController {

    private final CourseTagService courseTagService;

    public CourseTagController(CourseTagService courseTagService) {
        this.courseTagService = courseTagService;
    }

    @GetMapping
    @Operation(
            summary = "Получить связи курсов и тегов",
            description = "Возвращает все связи Course–Tag без возможности их изменения")
    @ApiResponse(responseCode = "200", description = "Связи получены")
    public List<CourseTagResponse> findAll() {
        return courseTagService.findAll();
    }
}
