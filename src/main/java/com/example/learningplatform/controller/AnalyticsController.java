package com.example.learningplatform.controller;

import com.example.learningplatform.dto.CourseAnalyticsResponse;
import com.example.learningplatform.dto.LeaderboardEntryResponse;
import com.example.learningplatform.dto.QuizAnalyticsResponse;
import com.example.learningplatform.dto.StudentAnalyticsResponse;
import com.example.learningplatform.exception.ApiError;
import com.example.learningplatform.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Агрегированная статистика обучения")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/students/{studentId}")
    @Operation(
            summary = "Получить успеваемость студента",
            description = "Собирает статистику курсов, проверенных заданий и попыток Quiz студента")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Аналитика получена"),
        @ApiResponse(responseCode = "400", description = "Пользователь не является студентом",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public StudentAnalyticsResponse getStudentAnalytics(@PathVariable Integer studentId) {
        return analyticsService.getStudentAnalytics(studentId);
    }

    @GetMapping("/courses/{courseId}")
    @Operation(
            summary = "Получить статистику курса",
            description = "Собирает статистику студентов, заданий, Quiz и отзывов курса")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Аналитика получена"),
        @ApiResponse(responseCode = "404", description = "Курс не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public CourseAnalyticsResponse getCourseAnalytics(@PathVariable Integer courseId) {
        return analyticsService.getCourseAnalytics(courseId);
    }

    @GetMapping("/quizzes/{quizId}")
    @Operation(
            summary = "Получить статистику Quiz",
            description = "Возвращает количество попыток, участников и распределение результатов Quiz")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Аналитика получена"),
        @ApiResponse(responseCode = "404", description = "Quiz не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public QuizAnalyticsResponse getQuizAnalytics(@PathVariable Integer quizId) {
        return analyticsService.getQuizAnalytics(quizId);
    }

    @GetMapping("/courses/{courseId}/leaderboard")
    @Operation(
            summary = "Получить рейтинг студентов курса",
            description = "Ранжирует активных и завершивших курс студентов по заданиям и лучшим попыткам Quiz")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Рейтинг получен"),
        @ApiResponse(responseCode = "404", description = "Курс не найден",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<LeaderboardEntryResponse> getCourseLeaderboard(@PathVariable Integer courseId) {
        return analyticsService.getCourseLeaderboard(courseId);
    }
}
