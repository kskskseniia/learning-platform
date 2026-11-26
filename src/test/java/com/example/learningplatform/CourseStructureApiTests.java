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
class CourseStructureApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesModuleCrudAndOrdersModulesInsideCourse() throws Exception {
        int courseId = createCourse("Module CRUD");

        int secondModuleId = createModule(courseId, "Advanced", 2, "Advanced topics");
        int firstModuleId = createModule(courseId, "Introduction", 1, null);

        mockMvc.perform(get("/api/courses/{courseId}/modules", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstModuleId))
                .andExpect(jsonPath("$[0].courseId").value(courseId))
                .andExpect(jsonPath("$[0].title").value("Introduction"))
                .andExpect(jsonPath("$[1].id").value(secondModuleId));

        mockMvc.perform(put("/api/modules/{id}", secondModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Advanced Java",
                                  "orderIndex": 3,
                                  "description": "Updated topics"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Advanced Java"))
                .andExpect(jsonPath("$.orderIndex").value(3))
                .andExpect(jsonPath("$.description").value("Updated topics"));

        mockMvc.perform(get("/api/modules/{id}", secondModuleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId));

        mockMvc.perform(delete("/api/modules/{id}", secondModuleId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/modules/{id}", secondModuleId))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesModuleTitleUniquenessInsideCourse() throws Exception {
        int firstCourseId = createCourse("Module Uniqueness First");
        int secondCourseId = createCourse("Module Uniqueness Second");
        int firstModuleId = createModule(firstCourseId, "Repeated module", 1, null);
        int secondModuleId = createModule(firstCourseId, "Other module", 2, null);

        mockMvc.perform(post("/api/courses/{courseId}/modules", firstCourseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Repeated module","orderIndex":3,"description":null}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Module with this title already exists in the course"));

        mockMvc.perform(put("/api/modules/{id}", secondModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Repeated module","orderIndex":2,"description":null}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/modules/{id}", firstModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Repeated module","orderIndex":1,"description":null}
                                """))
                .andExpect(status().isOk());

        createModule(secondCourseId, "Repeated module", 1, null);
    }

    @Test
    void providesLessonCrudInsideModule() throws Exception {
        int courseId = createCourse("Lesson CRUD");
        int moduleId = createModule(courseId, "Lesson Module", 1, "Module description");

        MvcResult result = mockMvc.perform(post("/api/modules/{moduleId}/lessons", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "First lesson",
                                  "content": "Lesson text",
                                  "videoUrl": "https://example.com/video"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value(moduleId))
                .andExpect(jsonPath("$.title").value("First lesson"))
                .andReturn();
        int lessonId = extractId(result);

        mockMvc.perform(get("/api/modules/{moduleId}/lessons", moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(lessonId))
                .andExpect(jsonPath("$[0].content").value("Lesson text"));

        mockMvc.perform(put("/api/lessons/{id}", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated lesson",
                                  "content": "Updated text",
                                  "videoUrl": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated lesson"))
                .andExpect(jsonPath("$.content").value("Updated text"))
                .andExpect(jsonPath("$.videoUrl").doesNotExist());

        mockMvc.perform(get("/api/lessons/{id}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleId").value(moduleId));

        mockMvc.perform(delete("/api/lessons/{id}", lessonId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/lessons/{id}", lessonId))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesLessonTitleUniquenessInsideModule() throws Exception {
        int courseId = createCourse("Lesson Uniqueness");
        int firstModuleId = createModule(courseId, "First lesson module", 1, null);
        int secondModuleId = createModule(courseId, "Second lesson module", 2, null);
        int firstLessonId = createLesson(firstModuleId, "Repeated lesson", null, null);
        int secondLessonId = createLesson(firstModuleId, "Other lesson", null, null);

        mockMvc.perform(post("/api/modules/{moduleId}/lessons", firstModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Repeated lesson","content":null,"videoUrl":null}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Lesson with this title already exists in the module"));

        mockMvc.perform(put("/api/lessons/{id}", secondLessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Repeated lesson","content":null,"videoUrl":null}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/lessons/{id}", firstLessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Repeated lesson","content":null,"videoUrl":null}
                                """))
                .andExpect(status().isOk());

        createLesson(secondModuleId, "Repeated lesson", null, null);
    }

    @Test
    void returnsOrderedCourseStructureWithLessons() throws Exception {
        int courseId = createCourse("Full Structure");
        int secondModuleId = createModule(courseId, "Second module", 2, null);
        int firstModuleId = createModule(courseId, "First module", 1, "First description");
        int firstLessonId = createLesson(firstModuleId, "First lesson", "First text", null);
        int secondLessonId = createLesson(firstModuleId, "Second lesson", null, "https://example.com/2");

        mockMvc.perform(get("/api/courses/{id}/structure", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.title").value("Full Structure Course"))
                .andExpect(jsonPath("$.modules[0].id").value(firstModuleId))
                .andExpect(jsonPath("$.modules[0].lessons[0].id").value(firstLessonId))
                .andExpect(jsonPath("$.modules[0].lessons[1].id").value(secondLessonId))
                .andExpect(jsonPath("$.modules[1].id").value(secondModuleId))
                .andExpect(jsonPath("$.modules[1].lessons").isEmpty());
    }

    @Test
    void rejectsInvalidStructureRequestsAndUnknownParents() throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/modules", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Module","orderIndex":1,"description":null}
                                """))
                .andExpect(status().isNotFound());

        int courseId = createCourse("Structure Validation");
        mockMvc.perform(post("/api/courses/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","orderIndex":null,"description":null}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/modules/{moduleId}/lessons", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Lesson","content":null,"videoUrl":null}
                                """))
                .andExpect(status().isNotFound());

        int moduleId = createModule(courseId, "Valid module", 1, null);
        mockMvc.perform(post("/api/modules/{moduleId}/lessons", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","content":null,"videoUrl":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesLessonsWithTheirModuleAndCourse() throws Exception {
        int courseId = createCourse("Cascade Delete");
        int firstModuleId = createModule(courseId, "First module", 1, null);
        int firstLessonId = createLesson(firstModuleId, "First lesson", null, null);

        mockMvc.perform(delete("/api/modules/{id}", firstModuleId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/lessons/{id}", firstLessonId))
                .andExpect(status().isNotFound());

        int secondModuleId = createModule(courseId, "Second module", 2, null);
        int secondLessonId = createLesson(secondModuleId, "Second lesson", null, null);
        mockMvc.perform(delete("/api/courses/{id}", courseId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/modules/{id}", secondModuleId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/lessons/{id}", secondLessonId))
                .andExpect(status().isNotFound());
    }

    private int createCourse(String suffix) throws Exception {
        int teacherId = createUser(
                suffix + " Teacher",
                suffix.toLowerCase().replace(' ', '-') + "@example.com",
                "TEACHER");
        int categoryId = createNamedResource("/api/categories", suffix + " Category");
        MvcResult result = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s Course",
                                  "description": "Course structure test",
                                  "categoryId": %d,
                                  "teacherId": %d,
                                  "duration": "4 weeks",
                                  "startDate": "2028-01-01",
                                  "tagIds": []
                                }
                                """.formatted(suffix, categoryId, teacherId)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int createModule(
            int courseId, String title, int orderIndex, String description) throws Exception {
        String descriptionJson = description == null ? "null" : "\"" + description + "\"";
        MvcResult result = mockMvc.perform(post("/api/courses/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "orderIndex": %d,
                                  "description": %s
                                }
                                """.formatted(title, orderIndex, descriptionJson)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private int createLesson(
            int moduleId, String title, String content, String videoUrl) throws Exception {
        String contentJson = content == null ? "null" : "\"" + content + "\"";
        String videoUrlJson = videoUrl == null ? "null" : "\"" + videoUrl + "\"";
        MvcResult result = mockMvc.perform(post("/api/modules/{moduleId}/lessons", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "content": %s,
                                  "videoUrl": %s
                                }
                                """.formatted(title, contentJson, videoUrlJson)))
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
}
