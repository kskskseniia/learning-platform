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
class CatalogApiTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesUserCrudWithoutAuthenticationFields() throws Exception {
        int userId = createUser("Анна", "anna-api@example.com", "STUDENT");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Анна"))
                .andExpect(jsonPath("$.email").value("anna-api@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Анна Петрова",
                                  "email": "anna-updated@example.com",
                                  "role": "TEACHER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Анна Петрова"))
                .andExpect(jsonPath("$.role").value("TEACHER"));

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateUserEmail() throws Exception {
        createUser("Анна", "duplicate@example.com", "STUDENT");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Иван",
                                  "email": "duplicate@example.com",
                                  "role": "TEACHER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with this email already exists"));
    }

    @Test
    void providesProfileCrudUsingUserId() throws Exception {
        int userId = createUser("Анна", "profile-api@example.com", "STUDENT");

        MvcResult result = mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "bio": "Java student",
                                  "avatarUrl": "https://example.com/avatar.png"
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andReturn();
        int profileId = extractId(result);

        mockMvc.perform(put("/api/profiles/{id}", profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bio": "Hibernate student",
                                  "avatarUrl": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Hibernate student"))
                .andExpect(jsonPath("$.avatarUrl").isEmpty());

        mockMvc.perform(delete("/api/profiles/{id}", profileId))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsProfileUserReassignment() throws Exception {
        int firstUserId = createUser("Первый", "first-profile-owner@example.com", "STUDENT");
        int secondUserId = createUser("Второй", "second-profile-owner@example.com", "STUDENT");
        int profileId = createProfile(firstUserId);

        mockMvc.perform(put("/api/profiles/{id}", profileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "bio": "Moved profile",
                                  "avatarUrl": null
                                }
                                """.formatted(secondUserId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/profiles/{id}", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(firstUserId));
    }

    @Test
    void deletesProfileWithItsUser() throws Exception {
        int userId = createUser("Удаляемый", "cascade-profile@example.com", "STUDENT");
        int profileId = createProfile(userId);

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/profiles/{id}", profileId))
                .andExpect(status().isNotFound());
    }

    @Test
    void providesCategoryAndTagCrud() throws Exception {
        int categoryId = createNamedResource("/api/categories", "Programming");
        int tagId = createNamedResource("/api/tags", "Java");

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(categoryId)).isNotEmpty());
        mockMvc.perform(get("/api/tags/{id}", tagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java"));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Databases\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Databases"));

        mockMvc.perform(delete("/api/tags/{id}", tagId))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsDuplicateCategoryNameIgnoringCaseOnCreate() throws Exception {
        createNamedResource("/api/categories", "Backend");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"backend\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Category with this name already exists"));
    }

    @Test
    void rejectsDuplicateCategoryNameIgnoringCaseOnUpdate() throws Exception {
        int originalCategoryId = createNamedResource("/api/categories", "Data Science");
        int categoryId = createNamedResource("/api/categories", "Machine Learning");

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"data science\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Category with this name already exists"));

        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Machine Learning"));

        mockMvc.perform(put("/api/categories/{id}", originalCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"data science\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("data science"));
    }

    @Test
    void rejectsDuplicateTagNameIgnoringCaseOnCreate() throws Exception {
        createNamedResource("/api/tags", "Spring");

        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"spring\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Tag with this name already exists"));
    }

    @Test
    void rejectsDuplicateTagNameIgnoringCaseOnUpdate() throws Exception {
        int originalTagId = createNamedResource("/api/tags", "PostgreSQL");
        int tagId = createNamedResource("/api/tags", "Hibernate");

        mockMvc.perform(put("/api/tags/{id}", tagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"postgresql\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Tag with this name already exists"));

        mockMvc.perform(get("/api/tags/{id}", tagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hibernate"));

        mockMvc.perform(put("/api/tags/{id}", originalTagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"postgresql\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("postgresql"));
    }

    @Test
    void validatesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-email\",\"role\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    void rejectsUnknownUserFields() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Eve","email":"eve@example.com","role":"STUDENT","password":"secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Malformed or unsupported request body"));
    }

    @Test
    void rejectsNamesLongerThanDatabaseColumn() throws Exception {
        String longName = "a".repeat(256);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name")
                        .value("Name must contain at most 255 characters"));
    }

    private int createUser(String name, String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "role": "%s"
                                }
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

    private int createProfile(int userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"bio":"Profile","avatarUrl":null}
                                """.formatted(userId)))
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
