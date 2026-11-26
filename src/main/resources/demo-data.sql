-- Demo data for manual Swagger testing.
-- Run explicitly with .\load-demo-data.ps1 after Flyway creates the schema.

BEGIN;

-- Users
INSERT INTO users (id, name, email, role) VALUES
    (1, 'Иван Иванов', 'ivan.ivanov@example.com', 'TEACHER'),
    (2, 'Петр Великий', 'petr.velikiy@example.com', 'TEACHER'),
    (3, 'Аня Соколова', 'anya.sokolova@example.com', 'STUDENT'),
    (4, 'Илья Волков', 'ilya.volkov@example.com', 'STUDENT'),
    (5, 'Мария Орлова', 'maria.orlova@example.com', 'STUDENT'),
    (6, 'Дмитрий Кузнецов', 'dmitry.kuznetsov@example.com', 'STUDENT')
ON CONFLICT DO NOTHING;

-- Profile
INSERT INTO profile (id, user_id, bio, avatar_url) VALUES
    (1, 1, 'Преподаватель дисциплин по Java и ORM', NULL)
ON CONFLICT DO NOTHING;

-- Category and tags
INSERT INTO category (id, name) VALUES
    (1, 'Разработка программного обеспечения'),
    (2, 'Информационная безопасность')
ON CONFLICT DO NOTHING;

INSERT INTO tag (id, name) VALUES
    (1, 'Java'),
    (2, 'Hibernate'),
    (3, 'Spring'),
    (4, 'REST API'),
    (5, 'JavaScript'),
    (6, 'Безопасность')
ON CONFLICT DO NOTHING;

-- Courses from the curriculum
INSERT INTO course (
    id, title, description, category_id, teacher_id, duration, start_date
) VALUES
    (1, 'ORM-фреймворки для Java', 'Работа с JPA и Hibernate', 1, 1, '72 часа', CURRENT_DATE),
    (2, 'Фреймворк Spring и работа с REST API', 'Создание REST-приложений на Spring Boot', 1, 1, '72 часа', CURRENT_DATE),
    (3, 'Разработка на JavaScript', 'Основы разработки приложений на JavaScript', 1, 2, '72 часа', CURRENT_DATE),
    (4, 'Сетевая информационная безопасность', 'Основы защиты сетей и сетевых сервисов', 2, 2, '72 часа', CURRENT_DATE)
ON CONFLICT DO NOTHING;

INSERT INTO course_tag (course_id, tag_id) VALUES
    (1, 1),
    (1, 2),
    (2, 1),
    (2, 3),
    (2, 4),
    (3, 5),
    (4, 6)
ON CONFLICT DO NOTHING;

-- Course structure
INSERT INTO module (id, course_id, title, order_index, description) VALUES
    (1, 1, 'Основы ORM', 1, 'Сущности и базовые отображения'),
    (2, 1, 'Связи между сущностями', 2, 'Отношения между объектами предметной области')
ON CONFLICT DO NOTHING;

INSERT INTO lesson (id, module_id, title, content, video_url) VALUES
    (1, 1, 'Что такое ORM', 'Назначение ORM и основы JPA', NULL),
    (2, 1, 'Первая JPA-сущность', 'Создание первой сущности с аннотацией Entity', NULL),
    (3, 2, 'Связь One-to-Many', 'Отображение связи один-ко-многим', NULL)
ON CONFLICT DO NOTHING;

-- Assignments: available, expired, and already graded
INSERT INTO assignment (
    id, lesson_id, title, description, due_date, max_score
) VALUES
    (1, 2, 'Создание JPA-сущности', 'Создать сущность и настроить отображение таблицы', CURRENT_DATE + 14, 100),
    (2, 1, 'Основные понятия ORM', 'Объяснить назначение ORM и JPA', CURRENT_DATE - 1, 50),
    (3, 3, 'Реализация связи One-to-Many', 'Настроить двунаправленную связь между сущностями', CURRENT_DATE + 30, 100)
ON CONFLICT DO NOTHING;

-- Enrollments: active students can submit; cancelled and unenrolled students cannot
INSERT INTO enrollment (
    id, student_id, course_id, enroll_date, status
) VALUES
    (1, 3, 1, CURRENT_DATE, 'ACTIVE'),
    (2, 4, 1, CURRENT_DATE, 'CANCELLED'),
    (3, 6, 1, CURRENT_DATE, 'ACTIVE')
ON CONFLICT DO NOTHING;

-- One graded solution leaves assignment 1 free for manual submission
INSERT INTO submission (
    id, assignment_id, student_id, submitted_at, content, score, feedback
) VALUES (
    1,
    3,
    6,
    CURRENT_TIMESTAMP,
    'Реализована двунаправленная связь One-to-Many',
    80,
    'Хорошая работа, добавьте интеграционный тест'
)
ON CONFLICT DO NOTHING;

-- One existing review; student 3 remains free for review creation tests
INSERT INTO course_review (
    id, course_id, student_id, rating, comment, created_at
) VALUES (
    1,
    1,
    6,
    5,
    'Понятный курс с полезными практическими заданиями',
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

-- Keep identity sequences ahead of explicit IDs
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval(pg_get_serial_sequence('profile', 'id'), (SELECT COALESCE(MAX(id), 1) FROM profile));
SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT COALESCE(MAX(id), 1) FROM category));
SELECT setval(pg_get_serial_sequence('tag', 'id'), (SELECT COALESCE(MAX(id), 1) FROM tag));
SELECT setval(pg_get_serial_sequence('course', 'id'), (SELECT COALESCE(MAX(id), 1) FROM course));
SELECT setval(pg_get_serial_sequence('module', 'id'), (SELECT COALESCE(MAX(id), 1) FROM module));
SELECT setval(pg_get_serial_sequence('lesson', 'id'), (SELECT COALESCE(MAX(id), 1) FROM lesson));
SELECT setval(pg_get_serial_sequence('assignment', 'id'), (SELECT COALESCE(MAX(id), 1) FROM assignment));
SELECT setval(pg_get_serial_sequence('enrollment', 'id'), (SELECT COALESCE(MAX(id), 1) FROM enrollment));
SELECT setval(pg_get_serial_sequence('submission', 'id'), (SELECT COALESCE(MAX(id), 1) FROM submission));
SELECT setval(pg_get_serial_sequence('course_review', 'id'), (SELECT COALESCE(MAX(id), 1) FROM course_review));

COMMIT;
