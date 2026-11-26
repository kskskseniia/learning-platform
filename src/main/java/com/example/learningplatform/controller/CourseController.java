package com.example.learningplatform.controller;

import com.example.learningplatform.dto.CourseRequest;
import com.example.learningplatform.dto.CourseResponse;
import com.example.learningplatform.dto.CourseStructureResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.CourseService;
import com.example.learningplatform.service.CourseStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Управление курсами и их тегами")
public class CourseController {

    private final CourseService courseService;
    private final CourseStructureService courseStructureService;

    public CourseController(
            CourseService courseService, CourseStructureService courseStructureService) {
        this.courseService = courseService;
        this.courseStructureService = courseStructureService;
    }

    @GetMapping
    @Operation(summary = "Получить курсы", description = "Возвращает все курсы")
    @ApiResponse(responseCode = "200", description = "Курсы получены")
    public List<CourseResponse> findAll() {
        return courseService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить курс", description = "Возвращает курс по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Курс найден"),
        @ApiResponse(
                responseCode = "404",
                description = "Курс не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public CourseResponse findById(@PathVariable Integer id) {
        return courseService.findById(id);
    }

    @GetMapping("/{id}/structure")
    @Operation(
            summary = "Получить структуру курса",
            description = "Возвращает модули курса и уроки каждого модуля")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Структура курса получена"),
        @ApiResponse(
                responseCode = "404",
                description = "Курс не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public CourseStructureResponse findStructure(@PathVariable Integer id) {
        return courseStructureService.findByCourseId(id);
    }

    @PostMapping
    @Operation(summary = "Создать курс", description = "Создаёт курс и назначает ему теги")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Курс создан"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(oneOf = {
                            ValidationError.class, ApiError.class
                        }))),
        @ApiResponse(
                responseCode = "404",
                description = "Категория, преподаватель или тег не найдены",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Курс с таким названием, преподавателем и датой уже существует",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Обновить курс",
            description = "Полностью обновляет курс и заменяет набор его тегов")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Курс обновлён"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(oneOf = {
                            ValidationError.class, ApiError.class
                        }))),
        @ApiResponse(
                responseCode = "404",
                description = "Курс или связанный ресурс не найдены",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Курс с таким названием, преподавателем и датой уже существует",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public CourseResponse update(
            @PathVariable Integer id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить курс",
            description = "Удаляет курс, его связи с тегами, модули и уроки")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Курс удалён"),
        @ApiResponse(
                responseCode = "404",
                description = "Курс не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
