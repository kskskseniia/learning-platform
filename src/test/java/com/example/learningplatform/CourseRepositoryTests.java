package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.learningplatform.entity.Category;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.Tag;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.repository.CategoryRepository;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.TagRepository;
import com.example.learningplatform.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CourseRepositoryTests {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsCourseAndExplicitCourseTagLinks() {
        Course course = createCourseWithOneTag();
        Integer tagId = course.getCourseTags().iterator().next().getTag().getId();

        Integer linkCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM course_tag WHERE course_id = ? AND tag_id = ?",
                Integer.class,
                course.getId(),
                tagId);

        assertThat(course.getId()).isNotNull();
        assertThat(linkCount).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateCourseByCaseInsensitiveTitleTeacherAndStartDateAtDatabaseLevel() {
        Category category = categoryRepository.saveAndFlush(new Category("Unique Course Category"));
        User teacher = userRepository.saveAndFlush(
                new User("Unique Course Teacher", "unique-course-teacher@example.com", Role.TEACHER));
        LocalDate startDate = LocalDate.of(2027, 8, 1);

        courseRepository.saveAndFlush(new Course(
                "Database Unique Course",
                "First description",
                category,
                teacher,
                "4 weeks",
                startDate));

        Course duplicate = new Course(
                "database unique course",
                "Different description",
                category,
                teacher,
                "8 weeks",
                startDate);

        assertThatThrownBy(() -> courseRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateCourseTagAtDatabaseLevel() {
        Course course = createCourseWithOneTag();
        Integer tagId = course.getCourseTags().iterator().next().getTag().getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO course_tag (course_id, tag_id) VALUES (?, ?)",
                        course.getId(),
                        tagId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void removesCourseTagLinksWhenCourseIsDeleted() {
        Course course = createCourseWithOneTag();
        Integer courseId = course.getId();

        courseRepository.delete(course);
        courseRepository.flush();

        Integer linkCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM course_tag WHERE course_id = ?",
                Integer.class,
                courseId);
        assertThat(linkCount).isZero();
    }

    private Course createCourseWithOneTag() {
        Category category = categoryRepository.saveAndFlush(new Category("Repository Category"));
        User teacher = userRepository.saveAndFlush(
                new User("Repository Teacher", "repository-teacher@example.com", Role.TEACHER));
        Tag tag = tagRepository.saveAndFlush(new Tag("Repository Tag"));
        Course course = courseRepository.saveAndFlush(new Course(
                "Repository Course",
                "Repository description",
                category,
                teacher,
                "3 weeks",
                LocalDate.of(2027, 5, 1)));
        course.replaceTags(List.of(tag));
        courseRepository.flush();
        return course;
    }
}
