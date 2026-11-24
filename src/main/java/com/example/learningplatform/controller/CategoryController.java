package com.example.learningplatform.controller;

import com.example.learningplatform.dto.NameRequest;
import com.example.learningplatform.dto.NamedResourceResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.service.CategoryService;
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
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Управление категориями курсов")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Получить категории", description = "Возвращает все категории курсов")
    @ApiResponse(responseCode = "200", description = "Категории получены")
    public List<NamedResourceResponse> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить категорию", description = "Возвращает категорию по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Категория найдена"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    public NamedResourceResponse findById(@PathVariable Integer id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Создать категорию", description = "Создаёт категорию курса")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Категория создана"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(
                responseCode = "409",
                description = "Категория с таким именем уже существует",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<NamedResourceResponse> create(@Valid @RequestBody NameRequest request) {
        NamedResourceResponse response = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/categories/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить категорию", description = "Полностью обновляет категорию")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Категория обновлена"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(
                responseCode = "409",
                description = "Категория с таким именем уже существует",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public NamedResourceResponse update(
            @PathVariable Integer id, @Valid @RequestBody NameRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить категорию", description = "Удаляет категорию по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Категория удалена"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "409", description = "Категория используется курсами")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
