package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.learningplatform.repository.AssignmentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AssignmentSubmissionDatabaseTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Integer courseId;
    private Integer moduleId;
    private Integer lessonId;
    private Integer assignmentId;
    private Integer studentId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        Integer teacherId = insertUser("TEACHER", "teacher-" + suffix + "@example.com");
        studentId = insertUser("STUDENT", "student-" + suffix + "@example.com");
        Integer categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES (?) RETURNING id",
                Integer.class,
                "Category " + suffix);
        courseId = jdbcTemplate.queryForObject(
                """
                INSERT INTO course
                    (title, description, category_id, teacher_id, duration, start_date)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Integer.class,
                "Course " + suffix,
                "Database constraints",
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
                "Module " + suffix,
                1);
        lessonId = jdbcTemplate.queryForObject(
                """
                INSERT INTO lesson (module_id, title)
                VALUES (?, ?) RETURNING id
                """,
                Integer.class,
                moduleId,
                "Lesson " + suffix);
        assignmentId = insertAssignment(100);
        insertSubmission();
    }

    @Test
    void databaseRejectsDuplicateSubmissionForStudentAndAssignment() {
        assertThatThrownBy(this::insertSubmission)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAssignmentCascadesToSubmission() {
        jdbcTemplate.update("DELETE FROM assignment WHERE id = ?", assignmentId);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM submission WHERE assignment_id = ?",
                Integer.class,
                assignmentId);
        assertThat(remaining).isZero();
    }

    @Test
    void databaseRejectsNonPositiveAssignmentMaxScore() {
        assertThatThrownBy(() -> insertAssignment(0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsNegativeSubmissionScore() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE submission SET score = -1 WHERE assignment_id = ?",
                assignmentId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateModuleTitleInsideCourse() {
        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM module WHERE id = ?", String.class, moduleId);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO module (course_id, title, order_index) VALUES (?, ?, ?)",
                courseId,
                title,
                2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateLessonTitleInsideModule() {
        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM lesson WHERE id = ?", String.class, lessonId);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO lesson (module_id, title) VALUES (?, ?)", moduleId, title))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateAssignmentTitleInsideLesson() {
        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM assignment WHERE id = ?", String.class, assignmentId);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO assignment (lesson_id, title, description, due_date, max_score)
                VALUES (?, ?, ?, ?, ?)
                """,
                lessonId,
                title,
                "Duplicate title",
                LocalDate.of(2099, 1, 1),
                100))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void assignmentWriteLockSerializesConcurrentOperations() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> transactionTemplate().executeWithoutResult(status -> {
                assignmentRepository.findByIdForUpdate(assignmentId).orElseThrow();
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> transactionTemplate().executeWithoutResult(status ->
                    assignmentRepository.findByIdForUpdate(assignmentId).orElseThrow()));

            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private Integer insertUser(String role, String email) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (name, email, role) VALUES (?, ?, ?) RETURNING id",
                Integer.class,
                role + " user",
                email,
                role);
    }

    private Integer insertAssignment(int maxScore) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO assignment (lesson_id, title, description, due_date, max_score)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """,
                Integer.class,
                lessonId,
                "Database assignment " + UUID.randomUUID(),
                "Database invariant test",
                LocalDate.of(2099, 1, 1),
                maxScore);
    }

    private void insertSubmission() {
        jdbcTemplate.update(
                """
                INSERT INTO submission
                    (assignment_id, student_id, submitted_at, content)
                VALUES (?, ?, ?, ?)
                """,
                assignmentId,
                studentId,
                LocalDateTime.now(),
                "Database submission");
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release assignment lock");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting to release assignment lock", exception);
        }
    }
}
