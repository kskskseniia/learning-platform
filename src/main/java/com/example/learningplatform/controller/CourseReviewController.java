package com.example.learningplatform.controller;

import com.example.learningplatform.dto.CourseReviewRequest;
import com.example.learningplatform.dto.CourseReviewResponse;
import com.example.learningplatform.dto.CourseReviewUpdateRequest;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.CourseReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-reviews")
@Tag(name = "Course Reviews", description = "Отзывы студентов о курсах")
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    public CourseReviewController(CourseReviewService courseReviewService) {
        this.courseReviewService = courseReviewService;
    }

    @PostMapping
    @Operation(
            summary = "Создать отзыв о курсе",
            description = "Создаёт отзыв студента о курсе с текущими датой и временем")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Отзыв создан"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или автор не является студентом",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(oneOf = {
                            ValidationError.class, ApiError.class
                        }))),
        @ApiResponse(
                responseCode = "404",
                description = "Курс или студент не найдены",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Такой текст отзыва уже существует у студента для этого курса",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CourseReviewResponse> create(
            @Valid @RequestBody CourseReviewRequest request) {
        CourseReviewResponse response = courseReviewService.create(request);
        return ResponseEntity.created(URI.create("/api/course-reviews/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Получить отзывы о курсах",
            description = "Возвращает все отзывы студентов о курсах")
    @ApiResponse(responseCode = "200", description = "Отзывы получены")
    public List<CourseReviewResponse> findAll() {
        return courseReviewService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить отзыв по идентификатору",
            description = "Возвращает отзыв о курсе по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Отзыв найден"),
        @ApiResponse(
                responseCode = "404",
                description = "Отзыв не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public CourseReviewResponse findById(@PathVariable Integer id) {
        return courseReviewService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Изменить отзыв о курсе",
            description = "Изменяет оценку и комментарий, сохраняя курс, автора и время создания")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Отзыв изменён"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные оценка или комментарий",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(oneOf = {
                            ValidationError.class, ApiError.class
                        }))),
        @ApiResponse(
                responseCode = "404",
                description = "Отзыв не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Такой текст отзыва уже существует у студента для этого курса",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public CourseReviewResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody CourseReviewUpdateRequest request) {
        return courseReviewService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Удалить отзыв о курсе",
            description = "Физически удаляет отзыв о курсе")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Отзыв удалён"),
        @ApiResponse(
                responseCode = "404",
                description = "Отзыв не найден",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public void delete(@PathVariable Integer id) {
        courseReviewService.delete(id);
    }
}
