package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.learningplatform.entity.Category;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.CourseReview;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.repository.CategoryRepository;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.CourseReviewRepository;
import com.example.learningplatform.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CourseReviewRepositoryTests {

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsCourseReviewWithExactErFields() {
        User student = userRepository.saveAndFlush(
                new User("Review Student", "review-student@example.com", Role.STUDENT));
        User teacher = userRepository.saveAndFlush(
                new User("Review Teacher", "review-teacher@example.com", Role.TEACHER));
        Category category = categoryRepository.saveAndFlush(new Category("Review Category"));
        Course course = courseRepository.saveAndFlush(new Course(
                "Review Course",
                "Review description",
                category,
                teacher,
                "6 weeks",
                LocalDate.of(2027, 10, 1)));
        LocalDateTime createdAt = LocalDateTime.of(2027, 10, 15, 12, 30);

        CourseReview review = courseReviewRepository.saveAndFlush(
                new CourseReview(course, student, 5, "Excellent course", createdAt));

        assertThat(review.getId()).isNotNull();
        assertThat(review.getCourse().getId()).isEqualTo(course.getId());
        assertThat(review.getStudent().getId()).isEqualTo(student.getId());
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Excellent course");
        assertThat(review.getCreatedAt()).isEqualTo(createdAt);

        assertThatThrownBy(() -> courseReviewRepository.saveAndFlush(
                new CourseReview(course, student, 6, "Invalid rating", createdAt)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
