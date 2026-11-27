package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.learningplatform.dto.CourseStructureResponse;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.service.CourseStructureService;
import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LazyLoadingTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseStructureService courseStructureService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void demonstratesLazyFailureAndControlledStructureLoading() {
        Integer courseId = insertCourseWithModule();

        Course detachedCourse = transactionTemplate.execute(status ->
                courseRepository.findById(courseId).orElseThrow());

        assertThatThrownBy(() -> detachedCourse.getModules().size())
                .isInstanceOf(LazyInitializationException.class);

        CourseStructureResponse structure = courseStructureService.findByCourseId(courseId);

        assertThat(structure.modules())
                .singleElement()
                .satisfies(module -> assertThat(module.title()).isEqualTo("Lazy module"));
    }

    private Integer insertCourseWithModule() {
        String suffix = UUID.randomUUID().toString();
        Integer teacherId = jdbcTemplate.queryForObject(
                "INSERT INTO users (name, email, role) VALUES (?, ?, 'TEACHER') RETURNING id",
                Integer.class,
                "Lazy Teacher",
                "lazy-" + suffix + "@example.com");
        Integer categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES (?) RETURNING id",
                Integer.class,
                "Lazy Category " + suffix);
        Integer courseId = jdbcTemplate.queryForObject(
                """
                INSERT INTO course (
                    title, description, category_id, teacher_id, duration, start_date
                ) VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Integer.class,
                "Lazy Course",
                "Lazy loading demonstration",
                categoryId,
                teacherId,
                "1 week",
                Date.valueOf(LocalDate.of(2027, 1, 1)));
        jdbcTemplate.update(
                """
                INSERT INTO module (course_id, title, order_index, description)
                VALUES (?, 'Lazy module', 1, 'Loaded explicitly by service')
                """,
                courseId);
        return courseId;
    }
}
