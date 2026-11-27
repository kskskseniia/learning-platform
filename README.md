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

| Сущность | Связи |
|---|---|
| `User` | 1-1 `Profile`; 1-M `Course` (преподаватель), `Enrollment`, `Submission`, `CourseReview`, `QuizSubmission` |
| `Profile` | 1-1 `User` |
| `Category` | 1-M `Course` |
| `Tag` | 1-M `CourseTag`; связь M-M с `Course` через `CourseTag` |
| `Course` | M-1 `Category`, `User` (преподаватель); 1-M `CourseTag`, `Module`, `Enrollment`, `CourseReview` |
| `CourseTag` | M-1 `Course`, `Tag`; уникальная пара курс-тег |
| `Module` | M-1 `Course`; 1-M `Lesson`; 1-1 `Quiz` |
| `Lesson` | M-1 `Module`; 1-M `Assignment` |
| `Assignment` | M-1 `Lesson`; 1-M `Submission` |
| `Submission` | M-1 `Assignment`, `User` (студент); уникальная пара студент-задание |
| `Enrollment` | M-1 `User` (студент), `Course`; уникальная пара студент-курс |
| `CourseReview` | M-1 `User` (студент), `Course` |
| `Quiz` | 1-1 `Module`; 1-M `Question`, `QuizSubmission` |
| `Question` | M-1 `Quiz`; 1-M `AnswerOption` |
| `AnswerOption` | M-1 `Question` |
| `QuizSubmission` | M-1 `Quiz`, `User` (студент) |

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

### Полный список эндпоинтов

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/users` | Получить всех пользователей |
| GET | `/api/users/{id}` | Получить пользователя |
| POST | `/api/users` | Создать пользователя |
| PUT | `/api/users/{id}` | Обновить пользователя |
| DELETE | `/api/users/{id}` | Удалить пользователя |
| GET | `/api/profiles` | Получить все профили |
| GET | `/api/profiles/{id}` | Получить профиль |
| POST | `/api/profiles` | Создать профиль пользователя |
| PUT | `/api/profiles/{id}` | Обновить профиль |
| DELETE | `/api/profiles/{id}` | Удалить профиль |
| GET | `/api/categories` | Получить все категории |
| GET | `/api/categories/{id}` | Получить категорию |
| POST | `/api/categories` | Создать категорию |
| PUT | `/api/categories/{id}` | Обновить категорию |
| DELETE | `/api/categories/{id}` | Удалить категорию |
| GET | `/api/tags` | Получить все теги |
| GET | `/api/tags/{id}` | Получить тег |
| POST | `/api/tags` | Создать тег |
| PUT | `/api/tags/{id}` | Обновить тег |
| DELETE | `/api/tags/{id}` | Удалить тег |
| GET | `/api/courses` | Получить все курсы |
| GET | `/api/courses/{id}` | Получить курс |
| GET | `/api/courses/{id}/structure` | Получить курс с модулями и уроками |
| POST | `/api/courses` | Создать курс и назначить теги |
| PUT | `/api/courses/{id}` | Обновить курс и его теги |
| DELETE | `/api/courses/{id}` | Удалить курс |
| GET | `/api/course-tags` | Получить связи курсов и тегов |
| GET | `/api/courses/{courseId}/modules` | Получить модули курса |
| POST | `/api/courses/{courseId}/modules` | Создать модуль курса |
| GET | `/api/modules/{id}` | Получить модуль |
| PUT | `/api/modules/{id}` | Обновить модуль |
| DELETE | `/api/modules/{id}` | Удалить модуль |
| GET | `/api/modules/{moduleId}/lessons` | Получить уроки модуля |
| POST | `/api/modules/{moduleId}/lessons` | Создать урок модуля |
| GET | `/api/lessons/{id}` | Получить урок |
| PUT | `/api/lessons/{id}` | Обновить урок |
| DELETE | `/api/lessons/{id}` | Удалить урок |
| GET | `/api/lessons/{lessonId}/assignments` | Получить задания урока |
| POST | `/api/lessons/{lessonId}/assignments` | Создать задание |
| GET | `/api/assignments/{id}` | Получить задание |
| PUT | `/api/assignments/{id}` | Обновить задание |
| DELETE | `/api/assignments/{id}` | Удалить задание |
| GET | `/api/assignments/{assignmentId}/submissions` | Получить решения задания |
| POST | `/api/assignments/{assignmentId}/submissions` | Отправить решение задания |
| GET | `/api/submissions/{id}` | Получить решение |
| PUT | `/api/submissions/{id}/grade` | Оценить решение |
| GET | `/api/enrollments` | Получить все записи на курсы |
| GET | `/api/enrollments/{id}` | Получить запись на курс |
| POST | `/api/enrollments` | Записать студента на курс |
| PUT | `/api/enrollments/{id}/status` | Изменить статус записи |
| DELETE | `/api/enrollments/{id}` | Удалить запись на курс |
| GET | `/api/course-reviews` | Получить все отзывы |
| GET | `/api/course-reviews/{id}` | Получить отзыв |
| POST | `/api/course-reviews` | Создать отзыв |
| PUT | `/api/course-reviews/{id}` | Обновить отзыв |
| DELETE | `/api/course-reviews/{id}` | Удалить отзыв |
| GET | `/api/modules/{moduleId}/quizzes` | Получить Quiz модуля |
| POST | `/api/modules/{moduleId}/quizzes` | Создать Quiz с вопросами и ответами |
| GET | `/api/quizzes/{id}` | Получить Quiz без признаков правильных ответов |
| PUT | `/api/quizzes/{id}` | Обновить Quiz до первой попытки |
| DELETE | `/api/quizzes/{id}?teacherId={teacherId}` | Удалить Quiz до первой попытки |
| GET | `/api/quizzes/{quizId}/submissions?teacherId={teacherId}` | Получить результаты Quiz |
| POST | `/api/quizzes/{quizId}/submissions` | Пройти Quiz и сохранить результат |

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
