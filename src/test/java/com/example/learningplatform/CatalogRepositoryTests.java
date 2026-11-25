package com.example.learningplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.learningplatform.entity.Category;
import com.example.learningplatform.entity.Profile;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.Tag;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.repository.CategoryRepository;
import com.example.learningplatform.repository.ProfileRepository;
import com.example.learningplatform.repository.TagRepository;
import com.example.learningplatform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CatalogRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsUserByEmail() {
        User saved = userRepository.saveAndFlush(
                new User("Анна", "anna@example.com", Role.STUDENT));

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByEmail("anna@example.com"))
                .containsSame(saved);
    }

    @Test
    void rejectsDuplicateUserEmail() {
        userRepository.saveAndFlush(new User("Анна", "same@example.com", Role.STUDENT));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("Иван", "same@example.com", Role.TEACHER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesAndFindsProfileByUserId() {
        User user = userRepository.saveAndFlush(
                new User("Анна", "profile@example.com", Role.STUDENT));
        Profile profile = profileRepository.saveAndFlush(
                new Profile(user, "Java student", "https://example.com/avatar.png"));

        assertThat(profile.getId()).isNotNull();
        assertThat(profileRepository.findByUserId(user.getId()))
                .containsSame(profile);
    }

    @Test
    void rejectsSecondProfileForSameUser() {
        User user = userRepository.saveAndFlush(
                new User("Иван", "duplicate-profile@example.com", Role.STUDENT));
        profileRepository.saveAndFlush(new Profile(user, "First profile", null));

        assertThatThrownBy(() -> profileRepository.saveAndFlush(
                new Profile(user, "Second profile", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void keepsProfileUserLazyAfterReload() {
        User user = userRepository.saveAndFlush(
                new User("Мария", "lazy-profile@example.com", Role.TEACHER));
        Profile saved = profileRepository.saveAndFlush(new Profile(user, null, null));
        entityManager.clear();

        Profile reloaded = profileRepository.findById(saved.getId()).orElseThrow();

        assertThat(entityManager.getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .isLoaded(reloaded, "user"))
                .isFalse();
    }

    @Test
    void providesCrudForCategoryAndTag() {
        Category category = categoryRepository.saveAndFlush(new Category("Programming"));
        Tag tag = tagRepository.saveAndFlush(new Tag("Java"));

        assertThat(categoryRepository.findById(category.getId())).containsSame(category);
        assertThat(tagRepository.findById(tag.getId())).containsSame(tag);
    }

}
