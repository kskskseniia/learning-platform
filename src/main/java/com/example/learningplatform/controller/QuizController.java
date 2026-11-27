package com.example.learningplatform.controller;

import com.example.learningplatform.dto.QuizRequest;
import com.example.learningplatform.dto.QuizResponse;
import com.example.learningplatform.dto.QuizSubmissionResponse;
import com.example.learningplatform.dto.TakeQuizRequest;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.exception.ValidationError;
import com.example.learningplatform.service.QuizService;
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
@Tag(name = "Quizzes", description = "Тесты знаний внутри модулей")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/modules/{moduleId}/quizzes")
    @Operation(
            summary = "Создать тест",
            description = "Создаёт тест вместе с вопросами и вариантами ответов")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Тест создан"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или преподаватель не владеет курсом",
                content = @Content(schema = @Schema(oneOf = {
                    ValidationError.class, ApiError.class
                }))),
        @ApiResponse(
                responseCode = "404",
                description = "Модуль не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Для модуля уже существует тест",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<QuizResponse> create(
            @PathVariable Integer moduleId,
            @Valid @RequestBody QuizRequest request) {
        QuizResponse response = quizService.create(moduleId, request);
        return ResponseEntity.created(URI.create("/api/quizzes/" + response.id())).body(response);
    }

    @GetMapping("/modules/{moduleId}/quizzes")
    @Operation(
            summary = "Получить тест модуля",
            description = "Возвращает список из одного теста или пустой список")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тест модуля или пустой список"),
        @ApiResponse(
                responseCode = "404",
                description = "Модуль не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<QuizResponse> findByModule(@PathVariable Integer moduleId) {
        return quizService.findByModule(moduleId);
    }

    @GetMapping("/quizzes/{id}")
    @Operation(
            summary = "Получить тест",
            description = "Возвращает вопросы и варианты без признака правильного ответа")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тест найден"),
        @ApiResponse(
                responseCode = "404",
                description = "Тест не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public QuizResponse findById(@PathVariable Integer id) {
        return quizService.findById(id);
    }

    @PutMapping("/quizzes/{id}")
    @Operation(
            summary = "Изменить тест целиком",
            description = "Полностью заменяет вопросы и ответы до появления первой попытки")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тест изменён"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные данные или преподаватель не владеет курсом",
                content = @Content(schema = @Schema(oneOf = {
                    ValidationError.class, ApiError.class
                }))),
        @ApiResponse(
                responseCode = "404",
                description = "Тест не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "У теста уже есть результаты",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public QuizResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody QuizRequest request) {
        return quizService.update(id, request);
    }

    @DeleteMapping("/quizzes/{id}")
    @Operation(
            summary = "Удалить тест",
            description = "Удаляет тест до появления первой попытки")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Тест удалён"),
        @ApiResponse(
                responseCode = "400",
                description = "Преподаватель не владеет курсом",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Тест не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "У теста уже есть результаты",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @RequestParam Integer teacherId) {
        quizService.delete(id, teacherId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quizzes/{quizId}/submissions")
    @Operation(
            summary = "Получить результаты теста",
            description = "Возвращает все попытки студентов преподавателю курса")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список результатов"),
        @ApiResponse(
                responseCode = "400",
                description = "Преподаватель не владеет курсом",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Тест не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<QuizSubmissionResponse> findSubmissions(
            @PathVariable Integer quizId,
            @RequestParam Integer teacherId) {
        return quizService.findSubmissions(quizId, teacherId);
    }

    @PostMapping("/quizzes/{quizId}/submissions")
    @Operation(
            summary = "Пройти тест",
            description = "Проверяет выбранные ответы и сохраняет результат студента")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Результат сохранён"),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные ответы или студент не записан на курс",
                content = @Content(schema = @Schema(oneOf = {
                    ValidationError.class, ApiError.class
                }))),
        @ApiResponse(
                responseCode = "404",
                description = "Тест или пользователь не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<QuizSubmissionResponse> submit(
            @PathVariable Integer quizId,
            @Valid @RequestBody TakeQuizRequest request) {
        QuizSubmissionResponse response = quizService.submit(quizId, request);
        return ResponseEntity.created(
                        URI.create("/api/quiz-submissions/" + response.id()))
                .body(response);
    }
}
