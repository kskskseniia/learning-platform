package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesProjectMetadataInOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Learning Platform API"))
                .andExpect(jsonPath("$.info.description").value("REST API учебной платформы"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void documentsEveryApiOperation() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        Map<String, Map<String, Map<String, Object>>> paths =
                JsonPath.parse(document).read("$.paths");
        Set<String> catalogPaths = Set.of(
                "/api/users",
                "/api/users/{id}",
                "/api/profiles",
                "/api/profiles/{id}",
                "/api/categories",
                "/api/categories/{id}",
                "/api/tags",
                "/api/tags/{id}",
                "/api/courses",
                "/api/courses/{id}",
                "/api/courses/{id}/structure",
                "/api/courses/{courseId}/modules",
                "/api/modules/{id}",
                "/api/modules/{moduleId}/lessons",
                "/api/lessons/{id}",
                "/api/lessons/{lessonId}/assignments",
                "/api/assignments/{id}",
                "/api/assignments/{assignmentId}/submissions",
                "/api/submissions/{id}",
                "/api/submissions/{id}/grade",
                "/api/course-tags",
                "/api/enrollments",
                "/api/enrollments/{id}",
                "/api/enrollments/{id}/status",
                "/api/course-reviews",
                "/api/course-reviews/{id}");
        Set<String> httpMethods = Set.of("get", "post", "put", "delete");
        List<Map<String, Object>> operations = catalogPaths.stream()
                .map(paths::get)
                .flatMap(path -> path.entrySet().stream())
                .filter(entry -> httpMethods.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        assertThat(paths).containsKeys(catalogPaths.toArray(String[]::new));
        assertThat(operations).hasSize(56);
        assertThat(operations).allSatisfy(operation -> {
            assertThat(operation.get("summary")).as("operation summary").isNotNull();
            assertThat(operation.get("description")).as("operation description").isNotNull();
            assertThat((Map<?, ?>) operation.get("responses"))
                    .as("documented responses")
                    .isNotEmpty();
        });
    }

    @Test
    void documentsConflictResponseBodies() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        List<String> conflictSchemas = List.of(
                "$.paths['/api/users/{id}'].put.responses['409'].content['application/json'].schema['$ref']",
                "$.paths['/api/categories'].post.responses['409'].content['application/json'].schema['$ref']",
                "$.paths['/api/categories/{id}'].put.responses['409'].content['application/json'].schema['$ref']",
                "$.paths['/api/tags'].post.responses['409'].content['application/json'].schema['$ref']",
                "$.paths['/api/tags/{id}'].put.responses['409'].content['application/json'].schema['$ref']",
                "$.paths['/api/courses'].post.responses['409'].content['application/json'].schema['$ref']",
                "$.paths['/api/courses/{id}'].put.responses['409'].content['application/json'].schema['$ref']");

        assertThat(conflictSchemas)
                .allSatisfy(path -> assertThat((String) JsonPath.parse(document).read(path))
                        .isEqualTo("#/components/schemas/ApiError"));
    }

    @Test
    void documentsSeparateProfileCreateAndUpdateRequests() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/profiles'].post.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ProfileRequest");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/profiles/{id}'].put.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ProfileUpdateRequest");
    }

    @Test
    void documentsCourseRequestAndResponseSchemas() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/courses'].post.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/CourseRequest");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/courses'].post.responses['201'].content['*/*'].schema['$ref']"))
                .isEqualTo("#/components/schemas/CourseResponse");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/courses/{id}'].get.responses['404'].content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ApiError");

        List<String> expectedBadRequestSchemas = List.of(
                "#/components/schemas/ApiError",
                "#/components/schemas/ValidationError");
        assertThat((List<String>) JsonPath.parse(document).read(
                "$.paths['/api/courses'].post.responses['400'].content['application/json'].schema.oneOf[*]['$ref']"))
                .containsExactlyInAnyOrderElementsOf(expectedBadRequestSchemas);
        assertThat((List<String>) JsonPath.parse(document).read(
                "$.paths['/api/courses/{id}'].put.responses['400'].content['application/json'].schema.oneOf[*]['$ref']"))
                .containsExactlyInAnyOrderElementsOf(expectedBadRequestSchemas);
        assertThat((List<String>) JsonPath.parse(document).read(
                "$.paths['/api/users/{id}'].put.responses['400'].content['application/json'].schema.oneOf[*]['$ref']"))
                .containsExactlyInAnyOrderElementsOf(expectedBadRequestSchemas);
        assertThat((List<String>) JsonPath.parse(document).read(
                "$.paths['/api/course-reviews/{id}'].put.responses['400'].content['application/json'].schema.oneOf[*]['$ref']"))
                .containsExactlyInAnyOrderElementsOf(expectedBadRequestSchemas);

        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/course-reviews'].post.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/CourseReviewRequest");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/course-reviews/{id}'].put.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/CourseReviewUpdateRequest");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/course-reviews'].post.responses['201'].content['*/*'].schema['$ref']"))
                .isEqualTo("#/components/schemas/CourseReviewResponse");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/course-reviews'].post.responses['409'].content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ApiError");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/course-reviews/{id}'].put.responses['409'].content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ApiError");

        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/course-tags'].get.responses['200'].content['*/*'].schema.items['$ref']"))
                .isEqualTo("#/components/schemas/CourseTagResponse");
    }

    @Test
    void documentsQuizOperationsWithoutExposingCorrectAnswers() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        List<String> quizOperations = List.of(
                "$.paths['/api/modules/{moduleId}/quizzes'].post",
                "$.paths['/api/modules/{moduleId}/quizzes'].get",
                "$.paths['/api/quizzes/{id}'].get",
                "$.paths['/api/quizzes/{id}'].put",
                "$.paths['/api/quizzes/{id}'].delete",
                "$.paths['/api/quizzes/{quizId}/submissions'].get",
                "$.paths['/api/quizzes/{quizId}/submissions'].post");

        assertThat(quizOperations).allSatisfy(path -> {
            Map<String, Object> operation = JsonPath.parse(document).read(path);
            assertThat(operation.get("summary")).as(path + " summary").isNotNull();
            assertThat(operation.get("description")).as(path + " description").isNotNull();
            assertThat((Map<?, ?>) operation.get("responses"))
                    .as(path + " responses")
                    .isNotEmpty();
        });
        assertThat((Map<String, Object>) JsonPath.parse(document).read(
                        "$.components.schemas.AnswerOptionResponse.properties"))
                .containsKeys("id", "text")
                .doesNotContainKey("correct");
        assertThat((String) JsonPath.parse(document).read(
                        "$.paths['/api/quizzes/{quizId}/submissions'].post"
                                + ".requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/TakeQuizRequest");
    }

    @Test
    void documentsAssignmentAndSubmissionSchemas() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/lessons/{lessonId}/assignments'].post.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/AssignmentRequest");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/lessons/{lessonId}/assignments'].post.responses['201'].content['*/*'].schema['$ref']"))
                .isEqualTo("#/components/schemas/AssignmentResponse");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/assignments/{assignmentId}/submissions'].post.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/SubmissionRequest");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/assignments/{assignmentId}/submissions'].post.responses['201'].content['*/*'].schema['$ref']"))
                .isEqualTo("#/components/schemas/SubmissionResponse");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/submissions/{id}/grade'].put.requestBody.content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/GradeSubmissionRequest");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/submissions/{id}/grade'].put.responses['200'].content['*/*'].schema['$ref']"))
                .isEqualTo("#/components/schemas/SubmissionResponse");
        assertThat((String) JsonPath.parse(document).read(
                "$.paths['/api/assignments/{assignmentId}/submissions'].post.responses['409'].content['*/*'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ApiError");
    }
}
