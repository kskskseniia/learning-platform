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

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CourseApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesCourseCrudAndReplacesTagsOnUpdate() throws Exception {
        int teacherId = createUser("Course Teacher", "course-teacher@example.com", "TEACHER");
        int categoryId = createNamedResource("/api/categories", "Course Category");
        int javaTagId = createNamedResource("/api/tags", "Course Java");
        int springTagId = createNamedResource("/api/tags", "Course Spring");

        MvcResult result = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java Developer",
                                  "description": "Java from basics to Spring",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "12 weeks",
                                  "startDate": "2027-01-15",
                                  "tagIds": [%d]
                                }
                                """.formatted(categoryId, teacherId, javaTagId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java Developer"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.teacherId").value(teacherId))
                .andExpect(jsonPath("$.tagIds[0]").value(javaTagId))
                .andReturn();
        int courseId = extractId(result);

        mockMvc.perform(get("/api/courses/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value("12 weeks"))
                .andExpect(jsonPath("$.startDate").value("2027-01-15"));

        mockMvc.perform(put("/api/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Developer",
                                  "description": "Spring Boot and persistence",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "8 weeks",
                                  "startDate": "2027-02-01",
                                  "tagIds": [%d]
                                }
                                """.formatted(categoryId, teacherId, springTagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Developer"))
                .andExpect(jsonPath("$.tagIds.length()").value(1))
                .andExpect(jsonPath("$.tagIds[0]").value(springTagId));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(courseId)).isNotEmpty());

        mockMvc.perform(get("/api/courses/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagIds.length()").value(1))
                .andExpect(jsonPath("$.tagIds[0]").value(springTagId));

        mockMvc.perform(delete("/api/courses/{id}", courseId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/courses/{id}", courseId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsCourseTeacherWithoutTeacherRole() throws Exception {
        int studentId = createUser("Course Student", "course-student@example.com", "STUDENT");
        int categoryId = createNamedResource("/api/categories", "Role Category");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid Teacher Course",
                                  "description": "Must not be created",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "1 week",
                                  "startDate": "2027-03-01",
                                  "tagIds": []
                                }
                                """.formatted(categoryId, studentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Course teacher must have TEACHER role"));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Invalid Teacher Course')]").isEmpty());
    }

    @Test
    void rejectsCourseWithMissingRelatedResources() throws Exception {
        int teacherId = createUser("Missing Relation Teacher", "missing-relations@example.com", "TEACHER");
        int categoryId = createNamedResource("/api/categories", "Missing Relation Category");

        performCreateCourse(999999, teacherId, "[]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with id 999999 not found"));

        performCreateCourse(categoryId, 999999, "[]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 999999 not found"));

        performCreateCourse(categoryId, teacherId, "[999999]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tag with id 999999 not found"));
    }

    @Test
    void validatesRequiredCourseFields() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists())
                .andExpect(jsonPath("$.errors.teacherId").exists())
                .andExpect(jsonPath("$.errors.duration").exists())
                .andExpect(jsonPath("$.errors.startDate").exists())
                .andExpect(jsonPath("$.errors.tagIds").exists());
    }

    @Test
    void protectsResourcesReferencedByCourseUntilCourseIsDeleted() throws Exception {
        int teacherId = createUser("Protected Teacher", "protected-teacher@example.com", "TEACHER");
        int categoryId = createNamedResource("/api/categories", "Protected Category");
        int tagId = createNamedResource("/api/tags", "Protected Tag");
        int courseId = extractId(performCreateCourse(categoryId, teacherId, "[" + tagId + "]")
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(delete("/api/users/{id}", teacherId))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/tags/{id}", tagId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/courses/{id}", courseId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/users/{id}", teacherId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/tags/{id}", tagId))
                .andExpect(status().isNoContent());
    }

    @Test
    void allowsUpdatingCourseWithUnchangedTagSet() throws Exception {
        int teacherId = createUser("Stable Tag Teacher", "stable-tag@example.com", "TEACHER");
        int categoryId = createNamedResource("/api/categories", "Stable Tag Category");
        int tagId = createNamedResource("/api/tags", "Stable Tag");
        int courseId = extractId(performCreateCourse(categoryId, teacherId, "[" + tagId + "]")
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(put("/api/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Stable Tag Course Updated",
                                  "description": "The tag set stays unchanged",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "4 weeks",
                                  "startDate": "2027-04-15",
                                  "tagIds": [%d]
                                }
                                """.formatted(categoryId, teacherId, tagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagIds.length()").value(1))
                .andExpect(jsonPath("$.tagIds[0]").value(tagId));

        mockMvc.perform(get("/api/courses/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Stable Tag Course Updated"))
                .andExpect(jsonPath("$.tagIds.length()").value(1));
    }

    @Test
    void preventsChangingRoleOfTeacherAssignedToCourse() throws Exception {
        int teacherId = createUser("Assigned Teacher", "assigned-teacher@example.com", "TEACHER");
        int categoryId = createNamedResource("/api/categories", "Assigned Teacher Category");
        int tagId = createNamedResource("/api/tags", "Assigned Teacher Tag");
        performCreateCourse(categoryId, teacherId, "[" + tagId + "]")
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/users/{id}", teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Assigned Teacher",
                                  "email": "assigned-teacher@example.com",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change role of a teacher assigned to courses"));

        mockMvc.perform(get("/api/users/{id}", teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void rejectsDuplicateCourseByTitleTeacherAndStartDate() throws Exception {
        int teacherId = createUser("Duplicate Teacher", "duplicate-teacher@example.com", "TEACHER");
        int firstCategoryId = createNamedResource("/api/categories", "Duplicate Category A");
        int secondCategoryId = createNamedResource("/api/categories", "Duplicate Category B");
        int tagId = createNamedResource("/api/tags", "Duplicate Tag");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duplicate Course",
                                  "description": "First version",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "4 weeks",
                                  "startDate": "2027-06-01",
                                  "tagIds": []
                                }
                                """.formatted(firstCategoryId, teacherId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "duplicate course",
                                  "description": "Different description and related data",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "12 weeks",
                                  "startDate": "2027-06-01",
                                  "tagIds": [%d]
                                }
                                """.formatted(secondCategoryId, teacherId, tagId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Course with the same title, teacher and start date already exists"));
    }

    @Test
    void rejectsCourseUpdateThatWouldCreateDuplicate() throws Exception {
        int teacherId = createUser("Update Duplicate Teacher", "update-duplicate@example.com", "TEACHER");
        int categoryId = createNamedResource("/api/categories", "Update Duplicate Category");

        MvcResult firstResult = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Existing Course",
                                  "description": "Existing",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "4 weeks",
                                  "startDate": "2027-07-01",
                                  "tagIds": []
                                }
                                """.formatted(categoryId, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        int firstCourseId = extractId(firstResult);

        MvcResult secondResult = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Course To Update",
                                  "description": "Must remain unchanged",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "6 weeks",
                                  "startDate": "2027-08-01",
                                  "tagIds": []
                                }
                                """.formatted(categoryId, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        int secondCourseId = extractId(secondResult);

        mockMvc.perform(put("/api/courses/{id}", secondCourseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "existing course",
                                  "description": "Attempted duplicate",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "10 weeks",
                                  "startDate": "2027-07-01",
                                  "tagIds": []
                                }
                                """.formatted(categoryId, teacherId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Course with the same title, teacher and start date already exists"));

        mockMvc.perform(get("/api/courses/{id}", secondCourseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Course To Update"))
                .andExpect(jsonPath("$.description").value("Must remain unchanged"))
                .andExpect(jsonPath("$.startDate").value("2027-08-01"));
        mockMvc.perform(get("/api/courses/{id}", firstCourseId))
                .andExpect(status().isOk());
    }

    @Test
    void listsCourseTagRelations() throws Exception {
        int teacherId = createUser("Relation List Teacher", "relation-list@example.com", "TEACHER");
        int categoryId = createNamedResource("/api/categories", "Relation List Category");
        int firstTagId = createNamedResource("/api/tags", "Relation List Tag A");
        int secondTagId = createNamedResource("/api/tags", "Relation List Tag B");
        int courseId = extractId(performCreateCourse(
                        categoryId, teacherId, "[" + firstTagId + "," + secondTagId + "]")
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(get("/api/course-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.courseId == %d && @.tagId == %d)]"
                                .formatted(courseId, firstTagId))
                        .isNotEmpty())
                .andExpect(jsonPath("$[?(@.courseId == %d && @.tagId == %d)]"
                                .formatted(courseId, secondTagId))
                        .isNotEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions performCreateCourse(
            int categoryId, int teacherId, String tagIds) throws Exception {
        return mockMvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Relation Test Course",
                          "description": "Relation validation",
                          "categoryId": %d,
                          "teacherId": %d,
                          "duration": "2 weeks",
                          "startDate": "2027-04-01",
                          "tagIds": %s
                        }
                        """.formatted(categoryId, teacherId, tagIds)));
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
}
