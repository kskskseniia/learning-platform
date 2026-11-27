DELETE FROM quiz_submission WHERE id BETWEEN 99001 AND 99003;
DELETE FROM course_review WHERE id BETWEEN 99001 AND 99002;
DELETE FROM submission WHERE id BETWEEN 99001 AND 99003;
DELETE FROM enrollment WHERE id BETWEEN 99001 AND 99003;
DELETE FROM quiz WHERE id = 99001;
DELETE FROM assignment WHERE id BETWEEN 99001 AND 99002;
DELETE FROM lesson WHERE id = 99001;
DELETE FROM module WHERE id = 99001;
DELETE FROM course WHERE id = 99001;
DELETE FROM category WHERE id = 99001;
DELETE FROM users WHERE id BETWEEN 99001 AND 99004;

INSERT INTO users (id, name, email, role) VALUES
    (99001, 'Analytics Teacher', 'analytics.teacher@example.com', 'TEACHER'),
    (99002, 'Analytics Alice', 'analytics.alice@example.com', 'STUDENT'),
    (99003, 'Analytics Bob', 'analytics.bob@example.com', 'STUDENT'),
    (99004, 'Analytics Cancelled', 'analytics.cancelled@example.com', 'STUDENT');
INSERT INTO category (id, name) VALUES (99001, 'Analytics Category');
INSERT INTO course (id, title, description, category_id, teacher_id, duration, start_date)
VALUES (99001, 'Analytics Course', 'Course for analytics tests', 99001, 99001, '4 weeks', '2026-01-10');
INSERT INTO module (id, course_id, title, order_index, description)
VALUES (99001, 99001, 'Analytics Module', 1, 'Module');
INSERT INTO lesson (id, module_id, title, content)
VALUES (99001, 99001, 'Analytics Lesson', 'Lesson');
INSERT INTO assignment (id, lesson_id, title, description, due_date, max_score) VALUES
    (99001, 99001, 'Assignment 100', 'Task', '2026-12-01', 100),
    (99002, 99001, 'Assignment 50', 'Task', '2026-12-02', 50);
INSERT INTO enrollment (id, student_id, course_id, enroll_date, status) VALUES
    (99001, 99002, 99001, '2026-01-10', 'ACTIVE'),
    (99002, 99003, 99001, '2026-01-10', 'COMPLETED'),
    (99003, 99004, 99001, '2026-01-10', 'CANCELLED');
INSERT INTO submission (id, assignment_id, student_id, submitted_at, content, score, feedback) VALUES
    (99001, 99001, 99002, '2026-01-20 10:00:00', 'Alice 100', 80, 'Good'),
    (99002, 99002, 99002, '2026-01-21 10:00:00', 'Alice 50', 40, 'Good'),
    (99003, 99001, 99003, '2026-01-20 11:00:00', 'Bob 100', 100, 'Excellent');
INSERT INTO quiz (id, module_id, title, time_limit)
VALUES (99001, 99001, 'Analytics Quiz', 20);
INSERT INTO quiz_submission (id, quiz_id, student_id, score, taken_at) VALUES
    (99001, 99001, 99002, 60, '2026-01-22 10:00:00'),
    (99002, 99001, 99002, 90, '2026-01-22 11:00:00'),
    (99003, 99001, 99003, 70, '2026-01-22 12:00:00');
INSERT INTO course_review (id, course_id, student_id, rating, comment, created_at) VALUES
    (99001, 99001, 99002, 5, 'Great', '2026-01-23 10:00:00'),
    (99002, 99001, 99003, 4, 'Good', '2026-01-23 11:00:00');
