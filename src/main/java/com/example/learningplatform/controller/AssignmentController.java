package com.example.learningplatform.controller;

import com.example.learningplatform.dto.AssignmentRequest;
import com.example.learningplatform.dto.AssignmentResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.AssignmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Assignments", description = "Задания внутри уроков")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/lessons/{lessonId}/assignments")
    @Operation(summary = "Создать задание", description = "Создаёт задание внутри указанного урока")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Задание создано"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или преподаватель не владеет курсом",
                content = @Content(schema = @Schema(oneOf = {
                    ValidationError.class, ApiError.class
                }))),
        @ApiResponse(
                responseCode = "404",
                description = "Урок не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AssignmentResponse> create(
            @PathVariable Integer lessonId,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = assignmentService.create(lessonId, request);
        return ResponseEntity.created(URI.create("/api/assignments/" + response.id())).body(response);
    }

    @GetMapping("/lessons/{lessonId}/assignments")
    @Operation(
            summary = "Получить задания урока",
            description = "Возвращает все задания указанного урока")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Задания получены"),
        @ApiResponse(
                responseCode = "404",
                description = "Урок не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<AssignmentResponse> findByLessonId(@PathVariable Integer lessonId) {
        return assignmentService.findByLessonId(lessonId);
    }

    @GetMapping("/assignments/{id}")
    @Operation(
            summary = "Получить задание",
            description = "Возвращает задание по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Задание найдено"),
        @ApiResponse(
                responseCode = "404",
                description = "Задание не найдено",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public AssignmentResponse findById(@PathVariable Integer id) {
        return assignmentService.findById(id);
    }

    @PutMapping("/assignments/{id}")
    @Operation(
            summary = "Обновить задание",
            description = "Обновляет задание от имени преподавателя-владельца курса")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Задание обновлено"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или преподаватель не владеет курсом",
                content = @Content(schema = @Schema(oneOf = {
                    ValidationError.class, ApiError.class
                }))),
        @ApiResponse(
                responseCode = "404",
                description = "Задание не найдено",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public AssignmentResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.update(id, request);
    }

    @DeleteMapping("/assignments/{id}")
    @Operation(
            summary = "Удалить задание",
            description = "Удаляет задание от имени преподавателя-владельца курса")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Задание удалено"),
        @ApiResponse(
                responseCode = "400",
                description = "Преподаватель не владеет курсом",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Задание не найдено",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(
            @PathVariable Integer id, @RequestParam Integer teacherId) {
        assignmentService.delete(id, teacherId);
        return ResponseEntity.noContent().build();
    }
}
