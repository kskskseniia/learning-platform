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
class EnrollmentApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enrollsStudentWithActiveStatus() throws Exception {
        int studentId = createUser("Enrollment API Student", "enrollment-api-student@example.com", "STUDENT");
        int teacherId = createUser("Enrollment API Teacher", "enrollment-api-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Enrollment API Category");
        int courseId = createCourse(categoryId, teacherId);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "courseId": %d
                                }
                                """.formatted(studentId, courseId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.enrollDate").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void readsUpdatesStatusAndDeletesEnrollment() throws Exception {
        int studentId = createUser("Lifecycle Student", "lifecycle-student@example.com", "STUDENT");
        int teacherId = createUser("Lifecycle Teacher", "lifecycle-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Lifecycle Category");
        int courseId = createCourse(categoryId, teacherId);
        int enrollmentId = createEnrollment(studentId, courseId);

        mockMvc.perform(get("/api/enrollments/{id}", enrollmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/enrollments/{id}/status", enrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d && @.status == 'COMPLETED')]".formatted(enrollmentId))
                        .isNotEmpty());

        mockMvc.perform(delete("/api/enrollments/{id}", enrollmentId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/enrollments/{id}", enrollmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesEnrollmentReferencesAndStatus() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":999999,\"courseId\":999999}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/enrollments/{id}/status", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
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
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int createCourse(int categoryId, int teacherId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Enrollment API Course",
                                  "description": "Course for enrollment tests",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "6 weeks",
                                  "startDate": "2027-10-01",
                                  "tagIds": []
                                }
                                """.formatted(categoryId, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
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

    private int extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Matcher matcher = ID_PATTERN.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Response does not contain id: " + body);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
