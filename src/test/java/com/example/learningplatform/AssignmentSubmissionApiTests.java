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
class AssignmentSubmissionApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesAssignmentCrudForCourseTeacher() throws Exception {
        int teacherId = createUser("Assignment Teacher", "assignment.teacher@example.com", "TEACHER");
        int lessonId = createCourseLesson(teacherId, "Assignment CRUD").lessonId();

        MvcResult createResult = mockMvc.perform(post("/api/lessons/{lessonId}/assignments", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teacherId": %d,
                                  "title": "REST homework",
                                  "description": "Build a REST endpoint",
                                  "dueDate": "2099-01-15",
                                  "maxScore": 100
                                }
                                """.formatted(teacherId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId").value(lessonId))
                .andExpect(jsonPath("$.title").value("REST homework"))
                .andExpect(jsonPath("$.maxScore").value(100))
                .andReturn();
        int assignmentId = extractId(createResult);

        mockMvc.perform(get("/api/lessons/{lessonId}/assignments", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assignmentId));
        mockMvc.perform(get("/api/assignments/{id}", assignmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2099-01-15"));

        mockMvc.perform(put("/api/assignments/{id}", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teacherId": %d,
                                  "title": "Updated homework",
                                  "description": "Updated description",
                                  "dueDate": "2099-02-01",
                                  "maxScore": 80
                                }
                                """.formatted(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated homework"))
                .andExpect(jsonPath("$.maxScore").value(80));

        mockMvc.perform(delete("/api/assignments/{id}", assignmentId)
                        .param("teacherId", String.valueOf(teacherId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/assignments/{id}", assignmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAssignmentCreationFromAnotherTeacher() throws Exception {
        int ownerId = createUser("Owner Teacher", "assignment.owner@example.com", "TEACHER");
        int otherTeacherId = createUser("Other Teacher", "assignment.other@example.com", "TEACHER");
        int lessonId = createCourseLesson(ownerId, "Assignment Owner").lessonId();

        mockMvc.perform(post("/api/lessons/{lessonId}/assignments", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teacherId": %d,
                                  "title": "Forbidden homework",
                                  "description": "Must not be created",
                                  "dueDate": "2099-01-15",
                                  "maxScore": 100
                                }
                                """.formatted(otherTeacherId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Only the course teacher can manage assignments"));
    }

    @Test
    void enforcesAssignmentTitleUniquenessInsideLesson() throws Exception {
        int teacherId = createUser(
                "Unique Assignment Teacher",
                "unique.assignment.teacher@example.com",
                "TEACHER");
        CourseLesson firstCourseLesson = createCourseLesson(teacherId, "Assignment Uniqueness First");
        CourseLesson secondCourseLesson = createCourseLesson(teacherId, "Assignment Uniqueness Second");
        int firstAssignmentId = createAssignment(
                teacherId, firstCourseLesson.lessonId(), "Repeated homework", "2099-01-15", 100);
        int secondAssignmentId = createAssignment(
                teacherId, firstCourseLesson.lessonId(), "Other homework", "2099-01-15", 100);

        mockMvc.perform(post("/api/lessons/{lessonId}/assignments", firstCourseLesson.lessonId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentRequest(teacherId, "Repeated homework")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Assignment with this title already exists in the lesson"));

        mockMvc.perform(put("/api/assignments/{id}", secondAssignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentRequest(teacherId, "Repeated homework")))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/assignments/{id}", firstAssignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignmentRequest(teacherId, "Repeated homework")))
                .andExpect(status().isOk());

        createAssignment(
                teacherId, secondCourseLesson.lessonId(), "Repeated homework", "2099-01-15", 100);
    }

    @Test
    void acceptsOneSubmissionFromActiveEnrolledStudent() throws Exception {
        int teacherId = createUser("Submit Teacher", "submit.teacher@example.com", "TEACHER");
        int studentId = createUser("Submit Student", "submit.student@example.com", "STUDENT");
        CourseLesson courseLesson = createCourseLesson(teacherId, "First Submission");
        createEnrollment(studentId, courseLesson.courseId());
        int assignmentId = createAssignment(
                teacherId, courseLesson.lessonId(), "2099-01-15", 100);

        String request = """
                {
                  "studentId": %d,
                  "content": "My completed homework"
                }
                """.formatted(studentId);
        MvcResult submitResult = mockMvc.perform(post(
                                "/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.content").value("My completed homework"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty())
                .andReturn();
        int submissionId = extractId(submitResult);

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions", assignmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(submissionId));
        mockMvc.perform(get("/api/submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("My completed homework"));

        mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Student has already submitted this assignment"));
    }

    @Test
    void rejectsSubmissionFromNonStudent() throws Exception {
        int teacherId = createUser("Role Teacher", "role.teacher@example.com", "TEACHER");
        CourseLesson courseLesson = createCourseLesson(teacherId, "Submission Role");
        int assignmentId = createAssignment(
                teacherId, courseLesson.lessonId(), "2099-01-15", 100);

        mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"content":"Not a student"}
                                """.formatted(teacherId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only students can submit assignments"));
    }

    @Test
    void rejectsSubmissionWithoutActiveEnrollment() throws Exception {
        int teacherId = createUser(
                "Enrollment Teacher", "enrollment.teacher@example.com", "TEACHER");
        int studentId = createUser(
                "Enrollment Student", "enrollment.student@example.com", "STUDENT");
        CourseLesson courseLesson = createCourseLesson(teacherId, "Inactive Enrollment");
        int enrollmentId = createEnrollment(studentId, courseLesson.courseId());
        mockMvc.perform(put("/api/enrollments/{id}/status", enrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        int assignmentId = createAssignment(
                teacherId, courseLesson.lessonId(), "2099-01-15", 100);

        mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"content":"Inactive enrollment"}
                                """.formatted(studentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Active enrollment in the course is required"));
    }

    @Test
    void rejectsSubmissionAfterAssignmentDeadline() throws Exception {
        int teacherId = createUser(
                "Deadline Teacher", "deadline.teacher@example.com", "TEACHER");
        int studentId = createUser(
                "Deadline Student", "deadline.student@example.com", "STUDENT");
        CourseLesson courseLesson = createCourseLesson(teacherId, "Past Deadline");
        createEnrollment(studentId, courseLesson.courseId());
        int assignmentId = createAssignment(
                teacherId, courseLesson.lessonId(), "2000-01-01", 100);

        mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"content":"Too late"}
                                """.formatted(studentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assignment deadline has passed"));

        mockMvc.perform(get("/api/assignments/{assignmentId}/submissions", assignmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void gradesSubmissionByCourseTeacher() throws Exception {
        int teacherId = createUser("Grade Teacher", "grade.teacher@example.com", "TEACHER");
        int studentId = createUser("Grade Student", "grade.student@example.com", "STUDENT");
        CourseLesson courseLesson = createCourseLesson(teacherId, "Submission Grade");
        createEnrollment(studentId, courseLesson.courseId());
        int assignmentId = createAssignment(
                teacherId, courseLesson.lessonId(), "2099-01-15", 100);
        MvcResult submissionResult = mockMvc.perform(post(
                                "/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"content":"Grade this homework"}
                                """.formatted(studentId)))
                .andExpect(status().isCreated())
                .andReturn();
        int submissionId = extractId(submissionResult);

        mockMvc.perform(put("/api/submissions/{id}/grade", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teacherId": %d,
                                  "score": 85,
                                  "feedback": "Good work"
                                }
                                """.formatted(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(85))
                .andExpect(jsonPath("$.feedback").value("Good work"));
    }

    @Test
    void rejectsGradeFromAnotherTeacher() throws Exception {
        int ownerId = createUser("Grade Owner", "grade.owner@example.com", "TEACHER");
        int otherTeacherId = createUser("Grade Other", "grade.other@example.com", "TEACHER");
        int studentId = createUser("Grade Owner Student", "grade.owner.student@example.com", "STUDENT");
        CourseLesson courseLesson = createCourseLesson(ownerId, "Grade Ownership");
        createEnrollment(studentId, courseLesson.courseId());
        int assignmentId = createAssignment(
                ownerId, courseLesson.lessonId(), "2099-01-15", 100);
        MvcResult submissionResult = mockMvc.perform(post(
                                "/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"content":"Ownership homework"}
                                """.formatted(studentId)))
                .andExpect(status().isCreated())
                .andReturn();
        int submissionId = extractId(submissionResult);

        mockMvc.perform(put("/api/submissions/{id}/grade", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId":%d,"score":70,"feedback":"Not allowed"}
                                """.formatted(otherTeacherId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Only the course teacher can grade submissions"));
    }

    @Test
    void enforcesGradeRangeFromZeroToAssignmentMaxScore() throws Exception {
        int teacherId = createUser("Range Teacher", "range.teacher@example.com", "TEACHER");
        int studentId = createUser("Range Student", "range.student@example.com", "STUDENT");
        CourseLesson courseLesson = createCourseLesson(teacherId, "Grade Range");
        createEnrollment(studentId, courseLesson.courseId());
        int assignmentId = createAssignment(
                teacherId, courseLesson.lessonId(), "2099-01-15", 100);
        MvcResult submissionResult = mockMvc.perform(post(
                                "/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"content":"Range homework"}
                                """.formatted(studentId)))
                .andExpect(status().isCreated())
                .andReturn();
        int submissionId = extractId(submissionResult);

        grade(submissionId, teacherId, 101)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Score must be between 0 and assignment max score"));
        grade(submissionId, teacherId, -1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Score must be between 0 and assignment max score"));
        grade(submissionId, teacherId, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0));
        grade(submissionId, teacherId, 100)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(100));
    }

    @Test
    void rejectsReducingMaxScoreBelowExistingGrade() throws Exception {
        int teacherId = createUser(
                "Max Score Teacher", "max.score.teacher@example.com", "TEACHER");
        int studentId = createUser(
                "Max Score Student", "max.score.student@example.com", "STUDENT");
        CourseLesson courseLesson = createCourseLesson(teacherId, "Max Score Update");
        createEnrollment(studentId, courseLesson.courseId());
        int assignmentId = createAssignment(
                teacherId, courseLesson.lessonId(), "2099-01-15", 100);
        MvcResult submissionResult = mockMvc.perform(post(
                                "/api/assignments/{assignmentId}/submissions", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"content":"Already graded homework"}
                                """.formatted(studentId)))
                .andExpect(status().isCreated())
                .andReturn();
        int submissionId = extractId(submissionResult);
        grade(submissionId, teacherId, 80).andExpect(status().isOk());

        mockMvc.perform(put("/api/assignments/{id}", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teacherId": %d,
                                  "title": "Updated homework",
                                  "description": "Updated description",
                                  "dueDate": "2099-01-15",
                                  "maxScore": 50
                                }
                                """.formatted(teacherId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Max score cannot be lower than an existing submission score"));
    }

    private org.springframework.test.web.servlet.ResultActions grade(
            int submissionId, int teacherId, int score) throws Exception {
        return mockMvc.perform(put("/api/submissions/{id}/grade", submissionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"teacherId":%d,"score":%d,"feedback":"Range check"}
                        """.formatted(teacherId, score)));
    }

    private CourseLesson createCourseLesson(int teacherId, String suffix) throws Exception {
        int categoryId = createNamedResource("/api/categories", suffix + " Category");
        MvcResult courseResult = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s Course",
                                  "description": "Assignment test course",
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
                                {
                                  "title": "%s Module",
                                  "orderIndex": 1,
                                  "description": null
                                }
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        int moduleId = extractId(moduleResult);

        MvcResult lessonResult = mockMvc.perform(post("/api/modules/{moduleId}/lessons", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s Lesson",
                                  "content": null,
                                  "videoUrl": null
                                }
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        return new CourseLesson(courseId, extractId(lessonResult));
    }

    private int createAssignment(
            int teacherId, int lessonId, String dueDate, int maxScore) throws Exception {
        return createAssignment(
                teacherId, lessonId, "Submission homework", dueDate, maxScore);
    }

    private int createAssignment(
            int teacherId,
            int lessonId,
            String title,
            String dueDate,
            int maxScore) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lessons/{lessonId}/assignments", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teacherId": %d,
                                  "title": "%s",
                                  "description": "Complete the homework",
                                  "dueDate": "%s",
                                  "maxScore": %d
                                }
                                """.formatted(teacherId, title, dueDate, maxScore)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private String assignmentRequest(int teacherId, String title) {
        return """
                {
                  "teacherId": %d,
                  "title": "%s",
                  "description": "Complete the homework",
                  "dueDate": "2099-01-15",
                  "maxScore": 100
                }
                """.formatted(teacherId, title);
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

    private record CourseLesson(int courseId, int lessonId) {
    }
}
