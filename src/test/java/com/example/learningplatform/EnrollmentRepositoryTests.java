package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.learningplatform.entity.Category;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.Enrollment;
import com.example.learningplatform.entity.EnrollmentStatus;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.repository.CategoryRepository;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.EnrollmentRepository;
import com.example.learningplatform.repository.UserRepository;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class EnrollmentRepositoryTests {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void persistsEnrollmentWithStudentCourseDateAndStatus() {
        User student = userRepository.saveAndFlush(
                new User("Enrollment Student", "enrollment-student@example.com", Role.STUDENT));
        User teacher = userRepository.saveAndFlush(
                new User("Enrollment Teacher", "enrollment-teacher@example.com", Role.TEACHER));
        Category category = categoryRepository.saveAndFlush(new Category("Enrollment Category"));
        Course course = courseRepository.saveAndFlush(new Course(
                "Enrollment Course",
                "Enrollment description",
                category,
                teacher,
                "5 weeks",
                LocalDate.of(2027, 9, 1)));

        Enrollment enrollment = enrollmentRepository.saveAndFlush(new Enrollment(
                student,
                course,
                LocalDate.of(2027, 8, 20),
                EnrollmentStatus.ACTIVE));

        assertThat(enrollment.getId()).isNotNull();
        assertThat(enrollment.getStudent().getId()).isEqualTo(student.getId());
        assertThat(enrollment.getCourse().getId()).isEqualTo(course.getId());
        assertThat(enrollment.getEnrollDate()).isEqualTo(LocalDate.of(2027, 8, 20));
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void serializesConcurrentChangesForTheSameStudent() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Integer studentId = transaction.execute(status -> userRepository.saveAndFlush(
                new User("Locked Student", "locked-student@example.com", Role.STUDENT)).getId());
        CountDownLatch firstTransactionLockedUser = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> firstTransaction = executor.submit(() -> transaction.executeWithoutResult(status -> {
                userRepository.findByIdForUpdate(studentId).orElseThrow();
                firstTransactionLockedUser.countDown();
                await(releaseFirstTransaction);
            }));
            assertThat(firstTransactionLockedUser.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> secondTransaction = executor.submit(() -> transaction.executeWithoutResult(status ->
                    userRepository.findByIdForUpdate(studentId).orElseThrow()));
            Thread.sleep(200);
            assertThat(secondTransaction).isNotDone();

            releaseFirstTransaction.countDown();
            firstTransaction.get(5, TimeUnit.SECONDS);
            secondTransaction.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
            transaction.executeWithoutResult(status -> userRepository.deleteById(studentId));
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release database lock");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to release database lock", exception);
        }
    }
}
