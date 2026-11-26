package com.example.learningplatform.controller;

import com.example.learningplatform.dto.ModuleRequest;
import com.example.learningplatform.dto.ModuleResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.ModuleService;
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
@Tag(name = "Modules", description = "Управление модулями курсов")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping("/courses/{courseId}/modules")
    @Operation(summary = "Создать модуль", description = "Создаёт модуль внутри указанного курса")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Модуль создан"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ValidationError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Курс не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ModuleResponse> create(
            @PathVariable Integer courseId, @Valid @RequestBody ModuleRequest request) {
        ModuleResponse response = moduleService.create(courseId, request);
        return ResponseEntity.created(URI.create("/api/modules/" + response.id())).body(response);
    }

    @GetMapping("/courses/{courseId}/modules")
    @Operation(summary = "Получить модули курса", description = "Возвращает модули курса по порядку")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Модули получены"),
        @ApiResponse(
                responseCode = "404",
                description = "Курс не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public List<ModuleResponse> findByCourseId(@PathVariable Integer courseId) {
        return moduleService.findByCourseId(courseId);
    }

    @GetMapping("/modules/{id}")
    @Operation(summary = "Получить модуль", description = "Возвращает модуль по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Модуль найден"),
        @ApiResponse(
                responseCode = "404",
                description = "Модуль не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ModuleResponse findById(@PathVariable Integer id) {
        return moduleService.findById(id);
    }

    @PutMapping("/modules/{id}")
    @Operation(summary = "Обновить модуль", description = "Полностью обновляет данные модуля")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Модуль обновлён"),
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
    public ModuleResponse update(
            @PathVariable Integer id, @Valid @RequestBody ModuleRequest request) {
        return moduleService.update(id, request);
    }

    @DeleteMapping("/modules/{id}")
    @Operation(summary = "Удалить модуль", description = "Удаляет модуль и его уроки")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Модуль удалён"),
        @ApiResponse(
                responseCode = "404",
                description = "Модуль не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        moduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
