package com.example.learningplatform.controller;

import com.example.learningplatform.dto.EnrollmentRequest;
import com.example.learningplatform.dto.EnrollmentResponse;
import com.example.learningplatform.dto.EnrollmentStatusRequest;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.EnrollmentService;
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
@RequestMapping("/api/enrollments")
@Tag(name = "Enrollments", description = "Запись студентов на курсы")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @Operation(
            summary = "Записать студента на курс",
            description = "Создаёт активную запись студента на курс с текущей датой")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Студент записан на курс"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или пользователь не является студентом",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(oneOf = {
                            ValidationError.class, ApiError.class
                        }))),
        @ApiResponse(
                responseCode = "404",
                description = "Студент или курс не найдены",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Студент уже записан на этот курс",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EnrollmentResponse> create(
            @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.create(request);
        return ResponseEntity.created(URI.create("/api/enrollments/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Получить список записей на курсы",
            description = "Возвращает все записи студентов на курсы")
    @ApiResponse(responseCode = "200", description = "Записи получены")
    public List<EnrollmentResponse> findAll() {
        return enrollmentService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить запись на курс по идентификатору",
            description = "Возвращает запись студента на курс по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запись найдена"),
        @ApiResponse(
                responseCode = "404",
                description = "Запись не найдена",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public EnrollmentResponse findById(@PathVariable Integer id) {
        return enrollmentService.findById(id);
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Изменить статус записи на курс",
            description = "Устанавливает статус ACTIVE, COMPLETED или CANCELLED")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Статус изменён"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректный статус",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(oneOf = {
                            ValidationError.class, ApiError.class
                        }))),
        @ApiResponse(
                responseCode = "404",
                description = "Запись не найдена",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public EnrollmentResponse updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody EnrollmentStatusRequest request) {
        return enrollmentService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Удалить запись на курс",
            description = "Физически удаляет запись студента на курс")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Запись удалена"),
        @ApiResponse(
                responseCode = "404",
                description = "Запись не найдена",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiError.class)))
    })
    public void delete(@PathVariable Integer id) {
        enrollmentService.delete(id);
    }
}
