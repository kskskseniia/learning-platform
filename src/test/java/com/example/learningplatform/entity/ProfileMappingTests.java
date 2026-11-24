package com.example.learningplatform.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class ProfileMappingTests {

    @Test
    void mapsOnlyFieldsFromErDiagram() {
        assertThat(Profile.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "user", "bio", "avatarUrl");
    }

    @Test
    void mapsProfileToUserWithUniqueForeignKey() throws NoSuchFieldException {
        Field userField = Profile.class.getDeclaredField("user");
        OneToOne relation = userField.getAnnotation(OneToOne.class);
        JoinColumn joinColumn = userField.getAnnotation(JoinColumn.class);

        assertThat(Profile.class.getAnnotation(Table.class).name()).isEqualTo("profile");
        assertThat(relation.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(joinColumn.name()).isEqualTo("user_id");
        assertThat(joinColumn.nullable()).isFalse();
        assertThat(joinColumn.unique()).isTrue();
    }

    @Test
    void createsProfileWithFieldsFromErDiagram() {
        User user = new User("Анна", "anna@example.com", Role.STUDENT);
        Profile profile = new Profile(user, "Java student", "https://example.com/avatar.png");

        assertThat(profile.getId()).isNull();
        assertThat(profile.getUser()).isSameAs(user);
        assertThat(profile.getBio()).isEqualTo("Java student");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
    }
}
