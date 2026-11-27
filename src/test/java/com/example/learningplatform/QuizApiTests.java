package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class QuizApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsQuizWithQuestionsAndAnswerOptions() throws Exception {
        int teacherId = createUser("Quiz Teacher", "quiz.teacher@example.com", "TEACHER");
        int moduleId = createCourseModule(teacherId, "Quiz create").moduleId();

        mockMvc.perform(post("/api/modules/{moduleId}/quizzes", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizRequest(teacherId, "Java basics")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value(moduleId))
                .andExpect(jsonPath("$.title").value("Java basics"))
                .andExpect(jsonPath("$.timeLimit").value(30))
                .andExpect(jsonPath("$.questions[0].text").value("Select JVM languages"))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].correct").doesNotExist());
    }

    @Test
    void calculatesQuizScoreForActiveStudent() throws Exception {
        int teacherId = createUser("Score Teacher", "score.teacher@example.com", "TEACHER");
        int studentId = createUser("Score Student", "score.student@example.com", "STUDENT");
        CourseModule courseModule = createCourseModule(teacherId, "Quiz score");
        createEnrollment(studentId, courseModule.courseId());
        CreatedQuiz quiz = createQuiz(teacherId, courseModule.moduleId(), "Scored quiz");

        mockMvc.perform(post("/api/quizzes/{quizId}/submissions", quiz.quizId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "selectedAnswers": {
                                    "%d": [%d, %d]
                                  }
                                }
                                """.formatted(
                                        studentId,
                                        quiz.questionId(),
                                        quiz.javaOptionId(),
                                        quiz.kotlinOptionId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quizId").value(quiz.quizId()))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.takenAt").isNotEmpty());
    }

    @Test
    void givesQuestionsEqualWeightAndRoundsScoreDown() throws Exception {
        int teacherId = createUser("Weighted Teacher", "weighted.teacher@example.com", "TEACHER");
        int studentId = createUser("Weighted Student", "weighted.student@example.com", "STUDENT");
        CourseModule courseModule = createCourseModule(teacherId, "Weighted quiz");
        createEnrollment(studentId, courseModule.courseId());

        MvcResult createResult = mockMvc.perform(post(
                                "/api/modules/{moduleId}/quizzes", courseModule.moduleId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(threeQuestionQuizRequest(teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = createResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        int quizId = extractId(createResult);
        int firstQuestionId = extractNestedId(body, "Question one");
        int firstCorrectOptionId = extractNestedId(body, "One correct");

        mockMvc.perform(post("/api/quizzes/{quizId}/submissions", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "selectedAnswers": {"%d": [%d]}
                                }
                                """.formatted(studentId, firstQuestionId, firstCorrectOptionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(33));
    }

    @Test
    void providesQuizAggregateCrudForCourseTeacher() throws Exception {
        int teacherId = createUser("CRUD Teacher", "crud.quiz.teacher@example.com", "TEACHER");
        CourseModule courseModule = createCourseModule(teacherId, "Quiz CRUD");
        CreatedQuiz quiz = createQuiz(teacherId, courseModule.moduleId(), "Original quiz");

        mockMvc.perform(get("/api/modules/{moduleId}/quizzes", courseModule.moduleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(quiz.quizId()));
        mockMvc.perform(get("/api/quizzes/{id}", quiz.quizId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Original quiz"))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].correct").doesNotExist());

        mockMvc.perform(put("/api/quizzes/{id}", quiz.quizId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizRequest(teacherId, "Updated quiz")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated quiz"));

        mockMvc.perform(delete("/api/quizzes/{id}", quiz.quizId())
                        .param("teacherId", String.valueOf(teacherId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/quizzes/{id}", quiz.quizId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void keepsAttemptHistoryAndLocksQuizContentAfterFirstSubmission() throws Exception {
        int teacherId = createUser("History Teacher", "history.teacher@example.com", "TEACHER");
        int studentId = createUser("History Student", "history.student@example.com", "STUDENT");
        CourseModule courseModule = createCourseModule(teacherId, "Quiz history");
        createEnrollment(studentId, courseModule.courseId());
        CreatedQuiz quiz = createQuiz(teacherId, courseModule.moduleId(), "History quiz");

        submitAnswers(studentId, quiz, "[%d, %d]".formatted(
                quiz.javaOptionId(), quiz.kotlinOptionId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(100));
        submitAnswers(studentId, quiz, "[]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(0));

        mockMvc.perform(get("/api/quizzes/{id}/submissions", quiz.quizId())
                        .param("teacherId", String.valueOf(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].score").value(100))
                .andExpect(jsonPath("$[1].score").value(0));

        mockMvc.perform(put("/api/quizzes/{id}", quiz.quizId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizRequest(teacherId, "Changed after attempt")))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/quizzes/{id}", quiz.quizId())
                        .param("teacherId", String.valueOf(teacherId)))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesQuizStructureAndOneQuizPerModule() throws Exception {
        int teacherId = createUser("Rules Teacher", "rules.teacher@example.com", "TEACHER");
        int moduleId = createCourseModule(teacherId, "Quiz rules").moduleId();

        mockMvc.perform(post("/api/modules/{moduleId}/quizzes", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleChoiceQuizRequest(teacherId, true, true)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/modules/{moduleId}/quizzes", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(multipleChoiceQuizRequest(teacherId, false, false)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/modules/{moduleId}/quizzes", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizRequest(teacherId, "Valid rules quiz")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/modules/{moduleId}/quizzes", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizRequest(teacherId, "Second quiz")))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsUnauthorizedQuizManagementAndInvalidSubmissions() throws Exception {
        int ownerId = createUser("Owner Teacher", "owner.quiz@example.com", "TEACHER");
        int otherTeacherId = createUser("Other Teacher", "other.quiz@example.com", "TEACHER");
        int studentId = createUser("Rules Student", "rules.student@example.com", "STUDENT");
        CourseModule courseModule = createCourseModule(ownerId, "Quiz permissions");

        mockMvc.perform(post("/api/modules/{moduleId}/quizzes", courseModule.moduleId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizRequest(otherTeacherId, "Forbidden quiz")))
                .andExpect(status().isBadRequest());
        CreatedQuiz quiz = createQuiz(ownerId, courseModule.moduleId(), "Protected quiz");

        submitAnswers(studentId, quiz, "[]")
                .andExpect(status().isBadRequest());
        createEnrollment(studentId, courseModule.courseId());
        mockMvc.perform(post("/api/quizzes/{quizId}/submissions", quiz.quizId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"selectedAnswers":{"999999":[]}}
                                """.formatted(studentId)))
                .andExpect(status().isBadRequest());
        submitAnswers(studentId, quiz, "[%d, %d]".formatted(
                quiz.javaOptionId(), quiz.javaOptionId()))
                .andExpect(status().isBadRequest());
        submitAnswers(studentId, quiz, "[999999]")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/quizzes/{id}/submissions", quiz.quizId())
                        .param("teacherId", String.valueOf(otherTeacherId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTeacherAndStudentWithInactiveEnrollment() throws Exception {
        int teacherId = createUser(
                "Role Teacher", "quiz.role.teacher@example.com", "TEACHER");
        int studentId = createUser("Inactive Student", "inactive.student@example.com", "STUDENT");
        CourseModule courseModule = createCourseModule(teacherId, "Quiz eligibility");
        CreatedQuiz quiz = createQuiz(teacherId, courseModule.moduleId(), "Eligibility quiz");

        submitAnswers(teacherId, quiz, "[]")
                .andExpect(status().isBadRequest());

        int enrollmentId = createEnrollment(studentId, courseModule.courseId());
        updateEnrollmentStatus(enrollmentId, "COMPLETED");
        submitAnswers(studentId, quiz, "[]")
                .andExpect(status().isBadRequest());

        updateEnrollmentStatus(enrollmentId, "CANCELLED");
        submitAnswers(studentId, quiz, "[]")
                .andExpect(status().isBadRequest());
    }

    private String threeQuestionQuizRequest(int teacherId) {
        return """
                {
                  "teacherId": %d,
                  "title": "Three equally weighted questions",
                  "timeLimit": 15,
                  "questions": [
                    {
                      "text": "Question one",
                      "type": "SINGLE_CHOICE",
                      "answerOptions": [
                        {"text":"One correct","correct":true},
                        {"text":"One wrong","correct":false}
                      ]
                    },
                    {
                      "text": "Question two",
                      "type": "SINGLE_CHOICE",
                      "answerOptions": [
                        {"text":"Two correct","correct":true},
                        {"text":"Two wrong","correct":false}
                      ]
                    },
                    {
                      "text": "Question three",
                      "type": "SINGLE_CHOICE",
                      "answerOptions": [
                        {"text":"Three correct","correct":true},
                        {"text":"Three wrong","correct":false}
                      ]
                    }
                  ]
                }
                """.formatted(teacherId);
    }

    private String singleChoiceQuizRequest(
            int teacherId, boolean firstCorrect, boolean secondCorrect) {
        return """
                {
                  "teacherId": %d,
                  "title": "Single choice rules",
                  "timeLimit": 15,
                  "questions": [{
                    "text": "Choose one",
                    "type": "SINGLE_CHOICE",
                    "answerOptions": [
                      {"text":"First","correct":%s},
                      {"text":"Second","correct":%s}
                    ]
                  }]
                }
                """.formatted(teacherId, firstCorrect, secondCorrect);
    }

    private String multipleChoiceQuizRequest(
            int teacherId, boolean firstCorrect, boolean secondCorrect) {
        return """
                {
                  "teacherId": %d,
                  "title": "Multiple choice rules",
                  "timeLimit": 15,
                  "questions": [{
                    "text": "Choose several",
                    "type": "MULTIPLE_CHOICE",
                    "answerOptions": [
                      {"text":"First","correct":%s},
                      {"text":"Second","correct":%s}
                    ]
                  }]
                }
                """.formatted(teacherId, firstCorrect, secondCorrect);
    }

    private org.springframework.test.web.servlet.ResultActions submitAnswers(
            int studentId, CreatedQuiz quiz, String answerIds) throws Exception {
        return mockMvc.perform(post("/api/quizzes/{quizId}/submissions", quiz.quizId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "studentId": %d,
                          "selectedAnswers": {"%d": %s}
                        }
                        """.formatted(studentId, quiz.questionId(), answerIds)));
    }

    private String quizRequest(int teacherId, String title) {
        return """
                {
                  "teacherId": %d,
                  "title": "%s",
                  "timeLimit": 30,
                  "questions": [
                    {
                      "text": "Select JVM languages",
                      "type": "MULTIPLE_CHOICE",
                      "answerOptions": [
                        {"text": "Java", "correct": true},
                        {"text": "Kotlin", "correct": true},
                        {"text": "Python", "correct": false}
                      ]
                    }
                  ]
                }
                """.formatted(teacherId, title);
    }

    private CreatedQuiz createQuiz(int teacherId, int moduleId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/modules/{moduleId}/quizzes", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizRequest(teacherId, title)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new CreatedQuiz(
                extractId(result),
                extractNestedId(body, "Select JVM languages"),
                extractNestedId(body, "Java"),
                extractNestedId(body, "Kotlin"));
    }

    private CourseModule createCourseModule(int teacherId, String suffix) throws Exception {
        int categoryId = createNamedResource("/api/categories", suffix + " Category");
        MvcResult courseResult = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s Course",
                                  "description": "Quiz test course",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "4 weeks",
                                  "startDate": "2028-01-01",
                                  "tagIds": []
                                }
                                """.formatted(suffix, categoryId, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        int courseId = extractId(courseResult);

        MvcResult moduleResult = mockMvc.perform(post("/api/courses/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s Module","orderIndex":1,"description":null}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        return new CourseModule(courseId, extractId(moduleResult));
    }

    private int createEnrollment(int studentId, int courseId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"courseId":%d}
                                """.formatted(studentId, courseId)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private void updateEnrollmentStatus(int enrollmentId, String status) throws Exception {
        mockMvc.perform(put("/api/enrollments/{id}/status", enrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"%s\"}".formatted(status)))
                .andExpect(status().isOk());
    }

    private int createUser(String name, String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","role":"%s"}
                                """.formatted(name, email, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int createNamedResource(String path, String name) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int extractId(MvcResult result) throws Exception {
        Matcher matcher = ID_PATTERN.matcher(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(matcher.find()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    private int extractNestedId(String body, String text) {
        Pattern pattern = Pattern.compile(
                "\\\"id\\\"\\s*:\\s*(\\d+)[^{}]*\\\"text\\\"\\s*:\\s*\\\""
                        + Pattern.quote(text)
                        + "\\\"");
        Matcher matcher = pattern.matcher(body);
        assertThat(matcher.find()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    private record CourseModule(int courseId, int moduleId) {
    }

    private record CreatedQuiz(
            int quizId, int questionId, int javaOptionId, int kotlinOptionId) {
    }
}
