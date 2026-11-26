package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DemoDataSqlTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDomainData() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE users, category, tag RESTART IDENTITY CASCADE");
    }

    @Test
    void demoDataLoadsTwiceWithoutDuplicates() {
        loadDemoData();
        loadDemoData();

        assertThat(count("""
                SELECT count(*)
                FROM users
                WHERE name IN (
                    'Иван Иванов',
                    'Петр Великий',
                    'Аня Соколова',
                    'Илья Волков',
                    'Мария Орлова',
                    'Дмитрий Кузнецов'
                )
                """))
                .isEqualTo(6);
        assertThat(count("""
                SELECT count(*)
                FROM course
                WHERE title IN (
                    'ORM-фреймворки для Java',
                    'Фреймворк Spring и работа с REST API',
                    'Разработка на JavaScript',
                    'Сетевая информационная безопасность'
                )
                """))
                .isEqualTo(4);
        assertThat(count("SELECT count(*) FROM module WHERE course_id = 1"))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM lesson WHERE module_id IN (1, 2)"))
                .isEqualTo(3);
        assertThat(count("SELECT count(*) FROM assignment WHERE id IN (1, 2, 3)"))
                .isEqualTo(3);
        assertThat(count("SELECT count(*) FROM enrollment WHERE course_id = 1"))
                .isEqualTo(3);
        assertThat(count("SELECT count(*) FROM enrollment WHERE student_id = 5"))
                .isZero();
        assertThat(count("SELECT count(*) FROM submission WHERE assignment_id = 3 AND score = 80"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM course_review WHERE course_id = 1 AND rating = 5"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM assignment WHERE id = 1 AND due_date > CURRENT_DATE"))
                .isEqualTo(1);
        assertThat(count("SELECT count(*) FROM assignment WHERE id = 2 AND due_date < CURRENT_DATE"))
                .isEqualTo(1);
    }

    private void loadDemoData() {
        new ResourceDatabasePopulator(new ClassPathResource("demo-data.sql"))
                .execute(dataSource);
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
