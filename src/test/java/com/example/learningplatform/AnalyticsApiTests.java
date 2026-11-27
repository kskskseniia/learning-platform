package com.example.learningplatform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Sql(scripts = "/analytics-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/analytics-test-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class AnalyticsApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStudentProgressAcrossCourses() throws Exception {
        mockMvc.perform(get("/api/analytics/students/99002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(99002))
                .andExpect(jsonPath("$.studentName").value("Analytics Alice"))
                .andExpect(jsonPath("$.enrolledCourses").value(1))
                .andExpect(jsonPath("$.activeCourses").value(1))
                .andExpect(jsonPath("$.completedCourses").value(0))
                .andExpect(jsonPath("$.submittedAssignments").value(2))
                .andExpect(jsonPath("$.gradedAssignments").value(2))
                .andExpect(jsonPath("$.averageAssignmentScore").value(80.0))
                .andExpect(jsonPath("$.quizAttempts").value(2))
                .andExpect(jsonPath("$.averageQuizScore").value(75.0))
                .andExpect(jsonPath("$.bestQuizScore").value(90));
    }

    @Test
    void returnsCourseAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics/courses/99001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseTitle").value("Analytics Course"))
                .andExpect(jsonPath("$.totalStudents").value(3))
                .andExpect(jsonPath("$.activeStudents").value(1))
                .andExpect(jsonPath("$.completedStudents").value(1))
                .andExpect(jsonPath("$.assignments").value(2))
                .andExpect(jsonPath("$.submissions").value(3))
                .andExpect(jsonPath("$.averageAssignmentScore").value(86.67))
                .andExpect(jsonPath("$.quizzes").value(1))
                .andExpect(jsonPath("$.quizAttempts").value(3))
                .andExpect(jsonPath("$.averageQuizScore").value(73.33))
                .andExpect(jsonPath("$.reviews").value(2))
                .andExpect(jsonPath("$.averageReviewRating").value(4.5));
    }

    @Test
    void returnsQuizStatistics() throws Exception {
        mockMvc.perform(get("/api/analytics/quizzes/99001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizTitle").value("Analytics Quiz"))
                .andExpect(jsonPath("$.attempts").value(3))
                .andExpect(jsonPath("$.uniqueStudents").value(2))
                .andExpect(jsonPath("$.averageScore").value(73.33))
                .andExpect(jsonPath("$.bestScore").value(90))
                .andExpect(jsonPath("$.worstScore").value(60));
    }

    @Test
    void ranksActiveAndCompletedStudentsWithinCourse() throws Exception {
        mockMvc.perform(get("/api/analytics/courses/99001/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].studentId").value(99002))
                .andExpect(jsonPath("$[0].assignmentAverage").value(80.0))
                .andExpect(jsonPath("$[0].quizAverage").value(90.0))
                .andExpect(jsonPath("$[0].overallScore").value(85.0))
                .andExpect(jsonPath("$[0].completedActivities").value(3))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].studentId").value(99003))
                .andExpect(jsonPath("$[1].overallScore").value(85.0));
    }

    @Test
    void returnsNotFoundForMissingAnalyticsResource() throws Exception {
        mockMvc.perform(get("/api/analytics/students/999999"))
                .andExpect(status().isNotFound());
    }
}
