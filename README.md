# Learning Platform

Учебный REST-сервис на Spring Boot, Spring Data JPA и PostgreSQL. Проект демонстрирует ER-модель образовательной платформы, CRUD, бизнес-правила, LAZY-загрузку, транзакции, Flyway, Swagger/OpenAPI и интеграционные тесты с Testcontainers.

## Стек

- Java 17
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway
- Bean Validation
- Springdoc OpenAPI / Swagger UI
- JUnit 5, MockMvc, Testcontainers
- Maven Wrapper

## Модель данных

В проекте 16 JPA-сущностей:

- `User`, `Profile`
- `Category`, `Tag`, `Course`, `CourseTag`
- `Module`, `Lesson`
- `Assignment`, `Submission`
- `Enrollment`, `CourseReview`
- `Quiz`, `Question`, `AnswerOption`, `QuizSubmission`

Основные отношения:

- `User 1 — 0..1 Profile`
- `Category 1 — N Course`
- `User(TEACHER) 1 — N Course`
- `Course N — M Tag` через явную сущность `CourseTag`
- `Course 1 — N Module 1 — N Lesson`
- `Lesson 1 — N Assignment 1 — N Submission`
- `User(STUDENT) N — M Course` через `Enrollment`
- `Course 1 — N CourseReview`
- `Module 1 — 0..1 Quiz 1 — N Question 1 — N AnswerOption`
- `Quiz 1 — N QuizSubmission`

Все ассоциации загружаются лениво. `spring.jpa.open-in-view=false`; работа с ассоциациями выполняется внутри сервисных транзакций. Тест `LazyLoadingTests` отдельно показывает `LazyInitializationException` после закрытия транзакции и корректную загрузку структуры через сервис.

## Запуск в PowerShell

Требования: JDK 17+ и запущенный Docker Desktop.

```powershell
Copy-Item .env.example .env
```

При необходимости измените пароль в `.env`. Запуск с сохранением существующих данных:

```powershell
.\start.ps1
```

Чистый запуск с удалением volume PostgreSQL:

```powershell
.\start-clean.ps1
```

После запуска приложения Swagger UI доступен по адресу:

- http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Демо-данные

В отдельном окне PowerShell после применения Flyway-миграции:

```powershell
.\load-demo-data.ps1
```

Скрипт идемпотентно добавляет преподавателей, студентов, курсы, теги, структуру курса, записи на курс, задания, решение с оценкой, отзыв и Quiz с двумя типами вопросов и завершённой попыткой.

Полезные ID для Swagger:

- преподаватель курса 1: `teacherId=1`;
- активный студент без готовой попытки Quiz: `studentId=3`;
- активный студент с существующей попыткой: `studentId=6`;
- отменённая запись: `studentId=4`;
- готовый Quiz: `quizId=1`, `moduleId=1`;
- свободный модуль для создания нового Quiz: `moduleId=2`.

## REST API

Полное интерактивное описание запросов, DTO, кодов ответа и ошибок находится в Swagger UI.

Основные группы маршрутов:

| Ресурс | Маршруты |
|---|---|
| Users | CRUD `/api/users` |
| Profiles | CRUD `/api/profiles` |
| Categories | CRUD `/api/categories` |
| Tags | CRUD `/api/tags` |
| Courses | CRUD `/api/courses`, `GET /api/courses/{id}/structure` |
| Course tags | `GET /api/course-tags` |
| Modules | `/api/courses/{courseId}/modules`, `/api/modules/{id}` |
| Lessons | `/api/modules/{moduleId}/lessons`, `/api/lessons/{id}` |
| Assignments | `/api/lessons/{lessonId}/assignments`, `/api/assignments/{id}` |
| Submissions | `/api/assignments/{assignmentId}/submissions`, `/api/submissions/{id}`, `PUT /api/submissions/{id}/grade` |
| Enrollments | `/api/enrollments`, `PUT /api/enrollments/{id}/status` |
| Reviews | CRUD `/api/course-reviews` |
| Quizzes | `/api/modules/{moduleId}/quizzes`, `/api/quizzes/{id}` |
| Quiz attempts | `POST/GET /api/quizzes/{quizId}/submissions` |

Учебный контракт не включает authentication/authorization framework. Идентификаторы преподавателя и студента передаются явно в запросах; сервис проверяет роли, владение курсом и активную запись.

## Ключевые бизнес-правила

- Только пользователь с ролью `TEACHER` может владеть курсом.
- Управлять модулями, уроками, заданиями и Quiz может только преподаватель соответствующего курса.
- Решение задания и Quiz принимает только `STUDENT` с активной записью на курс.
- Одно решение задания на пару студент/задание; повторные попытки Quiz разрешены.
- Оценка задания ограничена `0..maxScore`, оценка Quiz — `0..100`.
- После первой попытки Quiz его изменение и удаление запрещены.
- Правильные варианты ответа не возвращаются клиенту.
- Конкурирующие операции оценивания и Quiz защищены pessimistic write locks.
- Ограничения уникальности, диапазонов и каскадов продублированы на уровне PostgreSQL.

## Тесты

Docker Desktop должен быть запущен: интеграционные тесты поднимают реальный PostgreSQL через Testcontainers.

```powershell
.\mvnw.cmd test
```

Focused-примеры:

```powershell
.\mvnw.cmd -Dtest=LazyLoadingTests test
.\mvnw.cmd -Dtest=QuizApiTests,QuizDatabaseTests,QuizMappingTests test
```

Тесты покрывают CRUD, валидацию, ограничения БД, каскады, права по учебному контракту, дедлайны, оценивание, course structure, Quiz, OpenAPI и LAZY-поведение.

## Flyway и профили

- Единственная миграция: `src/main/resources/db/migration/V1__init.sql`.
- `dev`: параметры PostgreSQL берутся из `.env` через переменные `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- `test`: подключение выдаёт Testcontainers; Hibernate работает в режиме `validate`.
- Секреты в репозитории не хранятся.

## Docker и CI

Сборка образа приложения:

```powershell
docker build -t learning-platform .
```

`compose.yaml` поднимает PostgreSQL для локальной разработки. Workflow `.github/workflows/ci.yml` запускает полный Maven test suite на GitHub Actions; Docker на runner используется Testcontainers.
