package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class QuizDatabaseTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer moduleId;
    private Integer quizId;
    private Integer questionId;
    private Integer studentId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        Integer teacherId = insertUser("TEACHER", "quiz-db-teacher-" + suffix + "@example.com");
        studentId = insertUser("STUDENT", "quiz-db-student-" + suffix + "@example.com");
        Integer categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES (?) RETURNING id",
                Integer.class,
                "Quiz DB category " + suffix);
        Integer courseId = jdbcTemplate.queryForObject(
                """
                INSERT INTO course
                    (title, description, category_id, teacher_id, duration, start_date)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Integer.class,
                "Quiz DB course " + suffix,
                "Quiz database constraints",
                categoryId,
                teacherId,
                "1 week",
                LocalDate.of(2028, 1, 1));
        moduleId = jdbcTemplate.queryForObject(
                """
                INSERT INTO module (course_id, title, order_index)
                VALUES (?, ?, ?) RETURNING id
                """,
                Integer.class,
                courseId,
                "Quiz DB module " + suffix,
                1);
        quizId = insertQuiz(moduleId, 30);
        questionId = insertQuestion(quizId, "SINGLE_CHOICE");
        jdbcTemplate.update(
                "INSERT INTO answer_option (question_id, text, is_correct) VALUES (?, ?, ?)",
                questionId,
                "Correct option",
                true);
        insertSubmission(quizId, studentId, 100);
    }

    @Test
    void databaseAllowsSeveralAttemptsBySameStudent() {
        insertSubmission(quizId, studentId, 0);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM quiz_submission WHERE quiz_id = ? AND student_id = ?",
                Integer.class,
                quizId,
                studentId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void databaseRejectsSecondQuizForModule() {
        assertThatThrownBy(() -> insertQuiz(moduleId, 15))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseEnforcesQuizChecks() {
        Integer otherModuleId = duplicateModule("Invalid quiz module");
        assertThatThrownBy(() -> insertQuiz(otherModuleId, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertQuestion(quizId, "TEXT"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertSubmission(quizId, studentId, 101))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingModuleCascadesThroughWholeQuizAggregate() {
        jdbcTemplate.update("DELETE FROM module WHERE id = ?", moduleId);

        assertThat(count("quiz", "id", quizId)).isZero();
        assertThat(count("question", "quiz_id", quizId)).isZero();
        assertThat(count("answer_option", "question_id", questionId)).isZero();
        assertThat(count("quiz_submission", "quiz_id", quizId)).isZero();
    }

    @Test
    void deletingStudentCascadesToQuizSubmissionsOnly() {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);

        assertThat(count("quiz_submission", "student_id", studentId)).isZero();
        assertThat(count("quiz", "id", quizId)).isOne();
    }

    private Integer insertUser(String role, String email) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (name, email, role) VALUES (?, ?, ?) RETURNING id",
                Integer.class,
                role + " quiz database user",
                email,
                role);
    }

    private Integer insertQuiz(Integer targetModuleId, int timeLimit) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO quiz (module_id, title, time_limit)
                VALUES (?, ?, ?) RETURNING id
                """,
                Integer.class,
                targetModuleId,
                "Quiz " + UUID.randomUUID(),
                timeLimit);
    }

    private Integer insertQuestion(Integer targetQuizId, String type) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO question (quiz_id, text, type)
                VALUES (?, ?, ?) RETURNING id
                """,
                Integer.class,
                targetQuizId,
                "Question " + UUID.randomUUID(),
                type);
    }

    private void insertSubmission(Integer targetQuizId, Integer targetStudentId, int score) {
        jdbcTemplate.update(
                """
                INSERT INTO quiz_submission (quiz_id, student_id, score, taken_at)
                VALUES (?, ?, ?, ?)
                """,
                targetQuizId,
                targetStudentId,
                score,
                LocalDateTime.now());
    }

    private Integer duplicateModule(String title) {
        Integer courseId = jdbcTemplate.queryForObject(
                "SELECT course_id FROM module WHERE id = ?", Integer.class, moduleId);
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO module (course_id, title, order_index)
                VALUES (?, ?, ?) RETURNING id
                """,
                Integer.class,
                courseId,
                title + " " + UUID.randomUUID(),
                2);
    }

    private int count(String table, String column, Integer id) {
        String sql = "SELECT count(*) FROM " + table + " WHERE " + column + " = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, id);
    }
}
