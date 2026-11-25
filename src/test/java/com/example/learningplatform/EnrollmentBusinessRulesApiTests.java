package com.example.learningplatform;

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

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EnrollmentBusinessRulesApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsTeacherAndDuplicateEnrollments() throws Exception {
        int studentId = createUser("Rules Student", "rules-student@example.com", "STUDENT");
        int teacherId = createUser("Rules Teacher", "rules-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Rules Category");
        int courseId = createCourse("Rules Course", categoryId, teacherId);

        createEnrollment(studentId, courseId);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enrollmentJson(studentId, courseId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enrollmentJson(teacherId, courseId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blocksChangingEnrolledStudentToTeacher() throws Exception {
        int studentId = createUser("Role Student", "role-student@example.com", "STUDENT");
        int teacherId = createUser("Role Teacher", "role-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Enrollment Role Category");
        int courseId = createCourse("Role Course", categoryId, teacherId);
        createEnrollment(studentId, courseId);

        mockMvc.perform(put("/api/users/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Role Student","email":"role-student@example.com","role":"TEACHER"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/users/{id}", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void deletingStudentRemovesEnrollment() throws Exception {
        int studentId = createUser("Delete Student", "delete-student@example.com", "STUDENT");
        int teacherId = createUser("Delete Teacher", "delete-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Delete Student Category");
        int courseId = createCourse("Delete Student Course", categoryId, teacherId);
        int enrollmentId = createEnrollment(studentId, courseId);

        mockMvc.perform(delete("/api/users/{id}", studentId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/enrollments/{id}", enrollmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingCourseRemovesEnrollment() throws Exception {
        int studentId = createUser("Course Delete Student", "course-delete-student@example.com", "STUDENT");
        int teacherId = createUser("Course Delete Teacher", "course-delete-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Delete Course Category");
        int courseId = createCourse("Delete Enrollment Course", categoryId, teacherId);
        int enrollmentId = createEnrollment(studentId, courseId);

        mockMvc.perform(delete("/api/courses/{id}", courseId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/enrollments/{id}", enrollmentId))
                .andExpect(status().isNotFound());
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

    private int createCategory(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int createCourse(String title, int categoryId, int teacherId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "description":"Course for enrollment business rules",
                                  "categoryId":%d,
                                  "teacherId":%d,
                                  "duration":"4 weeks",
                                  "startDate":"2027-11-01",
                                  "tagIds":[]
                                }
                                """.formatted(title, categoryId, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int createEnrollment(int studentId, int courseId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enrollmentJson(studentId, courseId)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private String enrollmentJson(int studentId, int courseId) {
        return "{\"studentId\":%d,\"courseId\":%d}".formatted(studentId, courseId);
    }

    private int extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Matcher matcher = ID_PATTERN.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Response does not contain id: " + body);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
