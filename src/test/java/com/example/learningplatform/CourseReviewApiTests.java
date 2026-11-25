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
class CourseReviewApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsCourseReviewWithServerTimestamp() throws Exception {
        int studentId = createUser("Review API Student", "review-api-student@example.com", "STUDENT");
        int teacherId = createUser("Review API Teacher", "review-api-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Review API Category");
        int courseId = createCourse("Review API Course", categoryId, teacherId);

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, studentId, 5, "Excellent course")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent course"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void readsUpdatesAndDeletesCourseReview() throws Exception {
        int studentId = createUser("Review Lifecycle Student", "review-lifecycle-student@example.com", "STUDENT");
        int teacherId = createUser("Review Lifecycle Teacher", "review-lifecycle-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Review Lifecycle Category");
        int courseId = createCourse("Review Lifecycle Course", categoryId, teacherId);
        int reviewId = createReview(courseId, studentId, 4, "Good course");

        mockMvc.perform(get("/api/course-reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Good course"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(put("/api/course-reviews/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":\"Updated review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.rating").value(3))
                .andExpect(jsonPath("$.comment").value("Updated review"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/course-reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d && @.rating == 3)]".formatted(reviewId)).isNotEmpty());

        mockMvc.perform(delete("/api/course-reviews/{id}", reviewId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/course-reviews/{id}", reviewId))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/course-reviews/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Missing review\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/course-reviews/{id}", reviewId))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesCourseReviewReferencesAuthorAndFields() throws Exception {
        int teacherId = createUser("Invalid Review Teacher", "invalid-review-teacher@example.com", "TEACHER");
        int studentId = createUser("Invalid Review Student", "invalid-review-student@example.com", "STUDENT");
        int courseTeacherId = createUser("Review Course Teacher", "review-course-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Invalid Review Category");
        int courseId = createCourse("Invalid Review Course", categoryId, courseTeacherId);

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(999999, 999999, 5, "Missing references")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, teacherId, 5, "Teacher review")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, teacherId, 0, "Invalid rating")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, studentId, 6, "Invalid rating")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, teacherId, 5, " ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateNormalizedCommentForSameStudentAndCourse() throws Exception {
        int studentId = createUser(
                "Duplicate Review Student", "duplicate-review-student@example.com", "STUDENT");
        int otherStudentId = createUser(
                "Other Review Student", "other-review-student@example.com", "STUDENT");
        int teacherId = createUser(
                "Duplicate Review Teacher", "duplicate-review-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Duplicate Review Category");
        int courseId = createCourse("Duplicate Review Course", categoryId, teacherId);
        int firstReviewId = createReview(courseId, studentId, 5, "Repeated review text");

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, studentId, 3, "  repeated REVIEW text  ")))
                .andExpect(status().isConflict());

        int secondReviewId = createReview(courseId, studentId, 4, "Different review text");
        mockMvc.perform(put("/api/course-reviews/{id}", secondReviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":2,\"comment\":\" REPEATED REVIEW TEXT \"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/course-reviews/{id}", secondReviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Different review text"));

        mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, otherStudentId, 5, " repeated review text ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value("repeated review text"));

        mockMvc.perform(get("/api/course-reviews/{id}", firstReviewId))
                .andExpect(status().isOk());
    }

    @Test
    void blocksChangingReviewAuthorToTeacher() throws Exception {
        int studentId = createUser(
                "Review Role Student", "review-role-student@example.com", "STUDENT");
        int teacherId = createUser(
                "Review Role Teacher", "review-role-teacher@example.com", "TEACHER");
        int categoryId = createCategory("Review Role Category");
        int courseId = createCourse("Review Role Course", categoryId, teacherId);
        createReview(courseId, studentId, 5, "Role invariant review");

        mockMvc.perform(put("/api/users/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Review Role Student",
                                  "email":"review-role-student@example.com",
                                  "role":"TEACHER"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/users/{id}", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
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
                                  "description":"Course for review API tests",
                                  "categoryId":%d,
                                  "teacherId":%d,
                                  "duration":"5 weeks",
                                  "startDate":"2027-12-01",
                                  "tagIds":[]
                                }
                                """.formatted(title, categoryId, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int createReview(int courseId, int studentId, int rating, String comment) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/course-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(courseId, studentId, rating, comment)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private String reviewJson(int courseId, int studentId, int rating, String comment) {
        return """
                {"courseId":%d,"studentId":%d,"rating":%d,"comment":"%s"}
                """.formatted(courseId, studentId, rating, comment);
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
