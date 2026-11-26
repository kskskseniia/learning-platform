package com.example.learningplatform.controller;

import com.example.learningplatform.dto.GradeSubmissionRequest;
import com.example.learningplatform.dto.SubmissionRequest;
import com.example.learningplatform.dto.SubmissionResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.SubmissionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Submissions", description = "Решения заданий и их оценивание")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    @Operation(
            summary = "Отправить решение задания",
            description = "Создаёт единственное решение активного студента до окончания срока")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Решение отправлено"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или нарушено бизнес-правило",
                content = @Content(schema = @Schema(oneOf = {
                    ValidationError.class, ApiError.class
                }))),
        @ApiResponse(
                responseCode = "404",
                description = "Задание или пользователь не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Решение уже отправлено",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SubmissionResponse> create(
            @PathVariable Integer assignmentId,
            @Valid @RequestBody SubmissionRequest request) {
        SubmissionResponse response = submissionService.create(assignmentId, request);
        return ResponseEntity.created(URI.create("/api/submissions/" + response.id())).body(response);
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    @Operation(
            summary = "Получить решения задания",
            description = "Возвращает все решения указанного задания")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Решения получены"),
        @ApiResponse(
                responseCode = "404",
                description = "Задание не найдено",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<SubmissionResponse> findByAssignmentId(@PathVariable Integer assignmentId) {
        return submissionService.findByAssignmentId(assignmentId);
    }

    @GetMapping("/submissions/{id}")
    @Operation(
            summary = "Получить решение",
            description = "Возвращает решение по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Решение найдено"),
        @ApiResponse(
                responseCode = "404",
                description = "Решение не найдено",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public SubmissionResponse findById(@PathVariable Integer id) {
        return submissionService.findById(id);
    }

    @PutMapping("/submissions/{id}/grade")
    @Operation(
            summary = "Оценить решение",
            description = "Сохраняет оценку и отзыв преподавателя-владельца курса")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Оценка сохранена"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или нарушено бизнес-правило",
                content = @Content(schema = @Schema(oneOf = {
                    ValidationError.class, ApiError.class
                }))),
        @ApiResponse(
                responseCode = "404",
                description = "Решение не найдено",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public SubmissionResponse grade(
            @PathVariable Integer id,
            @Valid @RequestBody GradeSubmissionRequest request) {
        return submissionService.grade(id, request);
    }
}
