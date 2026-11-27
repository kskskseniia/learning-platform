package com.example.learningplatform.repository;

import com.example.learningplatform.dto.CourseAnalyticsResponse;
import com.example.learningplatform.dto.LeaderboardEntryResponse;
import com.example.learningplatform.dto.QuizAnalyticsResponse;
import com.example.learningplatform.dto.StudentAnalyticsResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StudentAnalyticsResponse getStudentAnalytics(Integer studentId, String studentName) {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM enrollment WHERE student_id = ?) AS enrolled_courses,
                    (SELECT COUNT(*) FROM enrollment WHERE student_id = ? AND status = 'ACTIVE') AS active_courses,
                    (SELECT COUNT(*) FROM enrollment WHERE student_id = ? AND status = 'COMPLETED') AS completed_courses,
                    (SELECT COUNT(*) FROM submission WHERE student_id = ?) AS submitted_assignments,
                    (SELECT COUNT(*) FROM submission WHERE student_id = ? AND score IS NOT NULL) AS graded_assignments,
                    COALESCE((SELECT ROUND(AVG(s.score * 100.0 / a.max_score), 2)
                        FROM submission s JOIN assignment a ON a.id = s.assignment_id
                        WHERE s.student_id = ? AND s.score IS NOT NULL), 0.00) AS assignment_average,
                    (SELECT COUNT(*) FROM quiz_submission WHERE student_id = ?) AS quiz_attempts,
                    COALESCE((SELECT ROUND(AVG(score), 2) FROM quiz_submission WHERE student_id = ?), 0.00) AS quiz_average,
                    (SELECT MAX(score) FROM quiz_submission WHERE student_id = ?) AS best_quiz_score
                """;
        Object[] args = {studentId, studentId, studentId, studentId, studentId,
                studentId, studentId, studentId, studentId};
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new StudentAnalyticsResponse(
                studentId,
                studentName,
                rs.getLong("enrolled_courses"),
                rs.getLong("active_courses"),
                rs.getLong("completed_courses"),
                rs.getLong("submitted_assignments"),
                rs.getLong("graded_assignments"),
                rs.getBigDecimal("assignment_average"),
                rs.getLong("quiz_attempts"),
                rs.getBigDecimal("quiz_average"),
                rs.getObject("best_quiz_score", Integer.class)), args);
    }

    public CourseAnalyticsResponse getCourseAnalytics(Integer courseId, String courseTitle) {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM enrollment WHERE course_id = ?) AS total_students,
                    (SELECT COUNT(*) FROM enrollment WHERE course_id = ? AND status = 'ACTIVE') AS active_students,
                    (SELECT COUNT(*) FROM enrollment WHERE course_id = ? AND status = 'COMPLETED') AS completed_students,
                    (SELECT COUNT(*) FROM assignment a JOIN lesson l ON l.id = a.lesson_id
                        JOIN module m ON m.id = l.module_id WHERE m.course_id = ?) AS assignments,
                    (SELECT COUNT(*) FROM submission s JOIN assignment a ON a.id = s.assignment_id
                        JOIN lesson l ON l.id = a.lesson_id JOIN module m ON m.id = l.module_id
                        WHERE m.course_id = ?) AS submissions,
                    (SELECT COUNT(*) FROM submission s JOIN assignment a ON a.id = s.assignment_id
                        JOIN lesson l ON l.id = a.lesson_id JOIN module m ON m.id = l.module_id
                        WHERE m.course_id = ? AND s.score IS NOT NULL) AS graded_submissions,
                    COALESCE((SELECT ROUND(AVG(s.score * 100.0 / a.max_score), 2)
                        FROM submission s JOIN assignment a ON a.id = s.assignment_id
                        JOIN lesson l ON l.id = a.lesson_id JOIN module m ON m.id = l.module_id
                        WHERE m.course_id = ? AND s.score IS NOT NULL), 0.00) AS assignment_average,
                    (SELECT COUNT(*) FROM quiz q JOIN module m ON m.id = q.module_id WHERE m.course_id = ?) AS quizzes,
                    (SELECT COUNT(*) FROM quiz_submission qs JOIN quiz q ON q.id = qs.quiz_id
                        JOIN module m ON m.id = q.module_id WHERE m.course_id = ?) AS quiz_attempts,
                    COALESCE((SELECT ROUND(AVG(qs.score), 2) FROM quiz_submission qs
                        JOIN quiz q ON q.id = qs.quiz_id JOIN module m ON m.id = q.module_id
                        WHERE m.course_id = ?), 0.00) AS quiz_average,
                    (SELECT COUNT(*) FROM course_review WHERE course_id = ?) AS reviews,
                    COALESCE((SELECT ROUND(AVG(rating), 2) FROM course_review WHERE course_id = ?), 0.00) AS review_average
                """;
        Object[] args = {courseId, courseId, courseId, courseId, courseId, courseId,
                courseId, courseId, courseId, courseId, courseId, courseId};
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new CourseAnalyticsResponse(
                courseId,
                courseTitle,
                rs.getLong("total_students"),
                rs.getLong("active_students"),
                rs.getLong("completed_students"),
                rs.getLong("assignments"),
                rs.getLong("submissions"),
                rs.getLong("graded_submissions"),
                rs.getBigDecimal("assignment_average"),
                rs.getLong("quizzes"),
                rs.getLong("quiz_attempts"),
                rs.getBigDecimal("quiz_average"),
                rs.getLong("reviews"),
                rs.getBigDecimal("review_average")), args);
    }

    public QuizAnalyticsResponse getQuizAnalytics(Integer quizId, String quizTitle) {
        String sql = """
                SELECT COUNT(*) AS attempts,
                       COUNT(DISTINCT student_id) AS unique_students,
                       COALESCE(ROUND(AVG(score), 2), 0.00) AS average_score,
                       MAX(score) AS best_score,
                       MIN(score) AS worst_score
                FROM quiz_submission WHERE quiz_id = ?
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new QuizAnalyticsResponse(
                quizId,
                quizTitle,
                rs.getLong("attempts"),
                rs.getLong("unique_students"),
                rs.getBigDecimal("average_score"),
                rs.getObject("best_score", Integer.class),
                rs.getObject("worst_score", Integer.class)), quizId);
    }

    public List<LeaderboardEntryResponse> getCourseLeaderboard(Integer courseId) {
        String sql = """
                WITH eligible AS (
                    SELECT u.id, u.name
                    FROM enrollment e JOIN users u ON u.id = e.student_id
                    WHERE e.course_id = ? AND e.status IN ('ACTIVE', 'COMPLETED')
                ), assignment_stats AS (
                    SELECT s.student_id,
                           ROUND(AVG(s.score * 100.0 / a.max_score), 2) AS assignment_average,
                           COUNT(*) AS completed_assignments
                    FROM submission s JOIN assignment a ON a.id = s.assignment_id
                    JOIN lesson l ON l.id = a.lesson_id JOIN module m ON m.id = l.module_id
                    WHERE m.course_id = ? AND s.score IS NOT NULL GROUP BY s.student_id
                ), quiz_best AS (
                    SELECT qs.student_id, qs.quiz_id, MAX(qs.score) AS best_score
                    FROM quiz_submission qs JOIN quiz q ON q.id = qs.quiz_id
                    JOIN module m ON m.id = q.module_id
                    WHERE m.course_id = ? GROUP BY qs.student_id, qs.quiz_id
                ), quiz_stats AS (
                    SELECT student_id, ROUND(AVG(best_score), 2) AS quiz_average,
                           COUNT(*) AS completed_quizzes
                    FROM quiz_best GROUP BY student_id
                ), scored AS (
                    SELECT e.id, e.name,
                           COALESCE(a.assignment_average, 0.00) AS assignment_average,
                           COALESCE(q.quiz_average, 0.00) AS quiz_average,
                           CASE WHEN a.assignment_average IS NOT NULL AND q.quiz_average IS NOT NULL
                                THEN ROUND((a.assignment_average + q.quiz_average) / 2, 2)
                                ELSE COALESCE(a.assignment_average, q.quiz_average, 0.00) END AS overall_score,
                           COALESCE(a.completed_assignments, 0) + COALESCE(q.completed_quizzes, 0) AS completed_activities
                    FROM eligible e LEFT JOIN assignment_stats a ON a.student_id = e.id
                    LEFT JOIN quiz_stats q ON q.student_id = e.id
                )
                SELECT ROW_NUMBER() OVER (ORDER BY overall_score DESC, completed_activities DESC, id ASC) AS rank,
                       id, name, assignment_average, quiz_average, overall_score, completed_activities
                FROM scored ORDER BY rank
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LeaderboardEntryResponse(
                rs.getLong("rank"),
                rs.getInt("id"),
                rs.getString("name"),
                rs.getBigDecimal("assignment_average"),
                rs.getBigDecimal("quiz_average"),
                rs.getBigDecimal("overall_score"),
                rs.getLong("completed_activities")), courseId, courseId, courseId);
    }
}
