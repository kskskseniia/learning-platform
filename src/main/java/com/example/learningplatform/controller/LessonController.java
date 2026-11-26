package com.example.learningplatform.controller;

import com.example.learningplatform.dto.LessonRequest;
import com.example.learningplatform.dto.LessonResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.LessonService;
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
@RequestMapping("/api")
@Tag(name = "Lessons", description = "Уроки внутри модулей курса")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @Operation(summary = "Создать урок", description = "Создаёт урок внутри указанного модуля")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Урок создан"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Модуль не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<LessonResponse> create(
            @PathVariable Integer moduleId, @Valid @RequestBody LessonRequest request) {
        LessonResponse response = lessonService.create(moduleId, request);
        return ResponseEntity.created(URI.create("/api/lessons/" + response.id())).body(response);
    }

    @GetMapping("/modules/{moduleId}/lessons")
    @Operation(summary = "Получить уроки модуля", description = "Возвращает уроки указанного модуля")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Уроки получены"),
        @ApiResponse(
                responseCode = "404",
                description = "Модуль не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public List<LessonResponse> findByModuleId(@PathVariable Integer moduleId) {
        return lessonService.findByModuleId(moduleId);
    }

    @GetMapping("/lessons/{id}")
    @Operation(summary = "Получить урок", description = "Возвращает урок по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Урок найден"),
        @ApiResponse(
                responseCode = "404",
                description = "Урок не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public LessonResponse findById(@PathVariable Integer id) {
        return lessonService.findById(id);
    }

    @PutMapping("/lessons/{id}")
    @Operation(summary = "Обновить урок", description = "Полностью обновляет данные урока")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Урок обновлён"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Урок не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public LessonResponse update(
            @PathVariable Integer id, @Valid @RequestBody LessonRequest request) {
        return lessonService.update(id, request);
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Удалить урок", description = "Удаляет урок из модуля")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Урок удалён"),
        @ApiResponse(
                responseCode = "404",
                description = "Урок не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
