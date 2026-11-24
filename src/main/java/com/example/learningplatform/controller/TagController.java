package com.example.learningplatform.controller;

import com.example.learningplatform.dto.NameRequest;
import com.example.learningplatform.dto.NamedResourceResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.service.TagService;
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
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Управление тегами курсов")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "Получить теги", description = "Возвращает все теги курсов")
    @ApiResponse(responseCode = "200", description = "Теги получены")
    public List<NamedResourceResponse> findAll() {
        return tagService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тег", description = "Возвращает тег по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тег найден"),
        @ApiResponse(responseCode = "404", description = "Тег не найден")
    })
    public NamedResourceResponse findById(@PathVariable Integer id) {
        return tagService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать тег", description = "Создаёт тег курса")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Тег создан"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(
                responseCode = "409",
                description = "Тег с таким именем уже существует",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<NamedResourceResponse> create(@Valid @RequestBody NameRequest request) {
        NamedResourceResponse response = tagService.create(request);
        return ResponseEntity.created(URI.create("/api/tags/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить тег", description = "Полностью обновляет тег")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тег обновлён"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "404", description = "Тег не найден"),
        @ApiResponse(
                responseCode = "409",
                description = "Тег с таким именем уже существует",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public NamedResourceResponse update(
            @PathVariable Integer id, @Valid @RequestBody NameRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тег", description = "Удаляет тег по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Тег удалён"),
        @ApiResponse(responseCode = "404", description = "Тег не найден"),
        @ApiResponse(responseCode = "409", description = "Тег используется курсами")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
